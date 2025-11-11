package backend;

import backend.dtos.InvoiceDto;
import backend.dtos.InvoiceRequest;
import backend.dtos.InvoiceUpdateRequest;
import backend.models.Invoice;
import backend.models.PaymentDetails;
import backend.models.Purchase;
import backend.models.User;
import backend.repositories.InvoiceRepository;
import backend.repositories.PurchaseRepository;
import backend.services.ResolveService;
import backend.services.invoice.InvoiceService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du service InvoiceService.
 *
 * Objectifs :
 *  - Vérifier la création de facture (référence, totaux initiaux, relations),
 *  - Vérifier les calculs de totaux HT/TTC,
 *  - Vérifier la mise à jour des totaux (persistance),
 *  - Vérifier les opérations de lecture (par id, toutes, par client),
 *  - Vérifier la mise à jour/confirmation d'une facture.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private ResolveService resolveService;

    @Mock
    private PurchaseRepository purchaseRepository;

    @InjectMocks
    private InvoiceService service;

    // ------------------------------------------------------------
    // Utilitaires de fabrication d'objets de test (purchases)
    // ------------------------------------------------------------
    private static Purchase p(BigDecimal totalHT, BigDecimal totalTVA) {
        Purchase x = new Purchase();
        x.setTotalHT(totalHT);
        x.setTotalTva(totalTVA);
        return x;
    }

    // =====================================================================================
    // saveInvoice
    // =====================================================================================
    @Test
    @DisplayName("saveInvoice : crée une facture avec référence et totaux initialisés")
    void saveInvoice_cree_facture_reference_et_totaux_zero() {
        // Arrange
        Long creatorId = 10L;
        Long customerId = 20L;
        Long paymentDetailsId = 30L;

        User creator = new User();
        creator.setId(creatorId);
        User customer = new User();
        customer.setId(customerId);
        PaymentDetails pd = new PaymentDetails();
        pd.setId(paymentDetailsId);

        when(resolveService.resolveUser(creatorId)).thenReturn(creator);
        when(resolveService.resolveUser(customerId)).thenReturn(customer);
        when(resolveService.resolvePaymentDetails(paymentDetailsId)).thenReturn(pd);

        // On simule l'absence de collision de référence
        when(invoiceRepository.findByReference(anyString())).thenReturn(Optional.empty());

        // Le save renvoie l'entité (avec un ID par exemple)
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice in = inv.getArgument(0);
            in.setId(99L);
            return in;
        });

        Date billing = new Date();
        Date due = new Date(billing.getTime() + 86400000L);

        InvoiceRequest req = new InvoiceRequest(
                billing,
                due,
                customerId,
                null,
                null,
                paymentDetailsId
        );

        // Act
        InvoiceDto out = service.saveInvoice(creatorId, req);

        // Assert
        assertThat(out).isNotNull();
        assertThat(out.id()).isEqualTo(99L);
        assertThat(out.billingDate()).isEqualTo(billing);
        assertThat(out.dueDate()).isEqualTo(due);
        assertThat(out.customerId()).isEqualTo(customerId);
        assertThat(out.creatorId()).isEqualTo(creatorId);
        assertThat(out.paymentDetailsId()).isEqualTo(paymentDetailsId);

        // La référence est générée au format ####-####
        assertThat(out.reference()).matches("\\d{4}-\\d{4}");

        // Totaux initialisés à 0
        assertThat(out.totalHT()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(out.totalTTC()).isEqualByComparingTo(BigDecimal.ZERO);

        verify(resolveService).resolveUser(creatorId);
        verify(resolveService).resolveUser(customerId);
        verify(resolveService).resolvePaymentDetails(paymentDetailsId);
        verify(invoiceRepository).save(any(Invoice.class));
    }

    // =====================================================================================
    // calculateTotalHT
    // =====================================================================================
    @Nested
    class CalculateTotalsTests {

        @Test
        @DisplayName("calculateTotalHT : somme des HT (ignore les null), arrondi 2 décimales")
        void calculateTotalHT_ok() {
            // Arrange
            Invoice inv = new Invoice();
            inv.setPurchases(Arrays.asList(
                    p(new BigDecimal("10.123"), new BigDecimal("2.00")),
                    p(new BigDecimal("5.50"), new BigDecimal("1.10")),
                    p(null, new BigDecimal("0.00")) // ignoré pour HT
            ));

            // Act
            BigDecimal totalHT = service.calculateTotalHT(inv);

            // Assert
            // (10.123 + 5.50) = 15.623 -> scale(2, HALF_UP) = 15.62
            assertThat(totalHT).isEqualByComparingTo(new BigDecimal("15.62"));
        }

        @Test
        @DisplayName("calculateTotalHT : 0 si pas d'achats")
        void calculateTotalHT_zero_si_aucun_achat() {
            Invoice inv = new Invoice();
            inv.setPurchases(Collections.emptyList());
            assertThat(service.calculateTotalHT(inv)).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("calculateTotalTTC : somme HT+TVA par ligne, arrondi 2 décimales")
        void calculateTotalTTC_ok() {
            // Arrange
            Invoice inv = new Invoice();
            inv.setPurchases(Arrays.asList(
                    p(new BigDecimal("10.10"), new BigDecimal("2.020")),
                    p(new BigDecimal("5.40"), new BigDecimal("1.080")),
                    p(null, new BigDecimal("9.99")) // devient 0 pour cette ligne
            ));

            // Act
            BigDecimal totalTTC = service.calculateTotalTTC(inv);

            // Assert
            // (10.10+2.020) + (5.40+1.080) + (0) = 18.60 -> scale(2) = 18.60
            assertThat(totalTTC).isEqualByComparingTo(new BigDecimal("18.60").setScale(2, RoundingMode.HALF_UP));
        }

        @Test
        @DisplayName("calculateTotalTTC : 0 si pas d'achats")
        void calculateTotalTTC_zero_si_aucun_achat() {
            Invoice inv = new Invoice();
            inv.setPurchases(Collections.emptyList());
            assertThat(service.calculateTotalTTC(inv)).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // =====================================================================================
    // updateInvoiceTotals
    // =====================================================================================
    @Test
    @DisplayName("updateInvoiceTotals : recalcule et persiste les totaux")
    void updateInvoiceTotals_recalcule_et_persiste() {
        // Arrange
        Invoice inv = new Invoice();
        inv.setPurchases(Arrays.asList(
                p(new BigDecimal("10.00"), new BigDecimal("2.00")),
                p(new BigDecimal("5.00"), new BigDecimal("1.00"))
        ));

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Invoice persisted = service.updateInvoiceTotals(inv);

        // Assert : HT = 15.00 ; TTC = (12 + 6) ? non : somme par ligne (10+2) + (5+1) = 18.00
        assertThat(persisted.getTotalHT()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(persisted.getTotalTTC()).isEqualByComparingTo(new BigDecimal("18.00"));

        verify(invoiceRepository).save(inv);
    }

    // =====================================================================================
    // getInvoice / getAllInvoices / getInvoicesOfCustomer
    // =====================================================================================
    @Test
    @DisplayName("getInvoice : retourne le DTO si trouvé, sinon null")
    void getInvoice_ok_ou_null() {
        Invoice inv = new Invoice();
        inv.setId(11L);
        when(invoiceRepository.findById(11L)).thenReturn(Optional.of(inv));
        when(invoiceRepository.findById(12L)).thenReturn(Optional.empty());

        assertThat(service.getInvoice(11L)).isNotNull();
        assertThat(service.getInvoice(12L)).isNull();

        verify(invoiceRepository).findById(11L);
        verify(invoiceRepository).findById(12L);
    }

    @Test
    @DisplayName("getAllInvoices : met à jour les totaux (non confirmée → 2 saves, confirmée → 1 save) puis renvoie les DTOs")
    void getAllInvoices_met_a_jour_totaux() {
        // Arrange : une facture non confirmée, une confirmée
        Invoice inv1 = new Invoice();
        inv1.setId(1L);
        inv1.setConfirmed(false);
        inv1.setPurchases(Collections.singletonList(p(new BigDecimal("10.00"), new BigDecimal("2.00"))));

        Invoice inv2 = new Invoice();
        inv2.setId(2L);
        inv2.setConfirmed(true);
        inv2.setPurchases(Collections.singletonList(p(new BigDecimal("5.00"), new BigDecimal("1.00"))));

        when(invoiceRepository.findAll()).thenReturn(List.of(inv1, inv2));
        // Chaque appel save renvoie l'argument
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<InvoiceDto> out = service.getAllInvoices();

        // Assert
        assertThat(out).hasSize(2);

        // Pour inv1 (non confirmée) : la méthode appelle update deux fois
        // → donc 2 saves attendus pour inv1
        // Pour inv2 (confirmée) : une seule fois
        verify(invoiceRepository, times(2)).save(argThat(i -> Objects.equals(i.getId(), 1L)));
        verify(invoiceRepository, times(1)).save(argThat(i -> Objects.equals(i.getId(), 2L)));
        verify(invoiceRepository).findAll();
    }

    @Test
    @DisplayName("getInvoicesOfCustomer : délègue au repository et mappe en DTO")
    void getInvoicesOfCustomer_ok() {
        Invoice invA = new Invoice();
        invA.setId(100L);
        invA.setConfirmed(true);
        Invoice invB = new Invoice();
        invB.setId(101L);
        invB.setConfirmed(true);

        when(invoiceRepository.findByCustomerIdAndIsConfirmedTrue(5L)).thenReturn(List.of(invA, invB));

        List<InvoiceDto> list = service.getInvoicesOfCustomer(5L);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).id()).isEqualTo(100L);
        assertThat(list.get(1).id()).isEqualTo(101L);

        verify(invoiceRepository).findByCustomerIdAndIsConfirmedTrue(5L);
    }

    // =====================================================================================
    // setInvoice
    // =====================================================================================
    @Test
    @DisplayName("setInvoice : confirme la facture → achats confirmés et sauvegardés, puis facture sauvegardée")
    void setInvoice_confirmation_confirme_achats_et_sauvegarde() {
        // Arrange
        Invoice invoice = new Invoice();
        invoice.setId(77L);

        Purchase a = new Purchase(); a.setConfirmed(false);
        Purchase b = new Purchase(); b.setConfirmed(false);
        invoice.setPurchases(Arrays.asList(a, b));

        when(invoiceRepository.findById(77L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceUpdateRequest req = new InvoiceUpdateRequest(
                null, // billingDate
                null, // dueDate
                null,
                true  // isConfirmed
        );

        // Act
        InvoiceDto out = service.setInvoice(77L, req);

        // Assert : chaque achat est confirmé
        assertThat(a.isConfirmed()).isTrue();
        assertThat(b.isConfirmed()).isTrue();

        // Les achats ont été persistés en lot
        verify(purchaseRepository).saveAll(invoice.getPurchases());
        // La facture est sauvegardée
        verify(invoiceRepository).save(invoice);

        // DTO retourné correspond à la facture
        assertThat(out.id()).isEqualTo(77L);
    }

    @Test
    @DisplayName("setInvoice : met à jour les dates et recalcule les totaux si non confirmé")
    void setInvoice_met_a_jour_dates_et_totaux_si_non_confirme() {
        // Arrange
        Invoice invoice = new Invoice();
        invoice.setId(88L);
        invoice.setPurchases(Collections.singletonList(p(new BigDecimal("10.00"), new BigDecimal("2.00"))));

        when(invoiceRepository.findById(88L)).thenReturn(Optional.of(invoice));
        // updateInvoiceTotals appelle save une première fois, ensuite setInvoice sauvegarde encore
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Date newBilling = new Date();
        Date newDue = new Date(newBilling.getTime() + 86400000L);

        InvoiceUpdateRequest req = new InvoiceUpdateRequest(
                newBilling,
                newDue,
                null,
                false
        );

        // Act
        InvoiceDto out = service.setInvoice(88L, req);

        // Assert : dates mises à jour
        assertThat(invoice.getBillingDate()).isEqualTo(newBilling);
        assertThat(invoice.getDueDate()).isEqualTo(newDue);

        // Totaux recalculés (10 + 2 = 12 TTC)
        assertThat(invoice.getTotalHT()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(invoice.getTotalTTC()).isEqualByComparingTo(new BigDecimal("12.00"));

        // Deux saves : un dans updateInvoiceTotals, un à la fin de setInvoice
        verify(invoiceRepository, atLeast(2)).save(invoice);

        assertThat(out.id()).isEqualTo(88L);
    }

    @Test
    @DisplayName("setInvoice : lève EntityNotFoundException si la facture est introuvable")
    void setInvoice_introuvable_declenche_exception() {
        when(invoiceRepository.findById(123L)).thenReturn(Optional.empty());

        InvoiceUpdateRequest req = new InvoiceUpdateRequest(null, null, null,false);

        assertThatThrownBy(() -> service.setInvoice(123L, req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Invoice not found");

        verify(invoiceRepository).findById(123L);
        verifyNoMoreInteractions(invoiceRepository, purchaseRepository);
    }
}
