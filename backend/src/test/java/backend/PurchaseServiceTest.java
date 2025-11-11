package backend;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import backend.dtos.PurchaseDto;
import backend.models.Invoice;
import backend.models.Product;
import backend.models.Purchase;
import backend.models.Tva;
import backend.models.User;
import backend.repositories.PurchaseRepository;
import backend.services.ResolveService;
import backend.services.invoice.InvoiceService;
import backend.services.purchase.PurchaseService;
import jakarta.persistence.EntityNotFoundException;

/**
 * Tests unitaires de {@link PurchaseService}.
 *
 * Stratégie :
 *  - savePurchase : normalisation à partir du produit et du taux de TVA par défaut.
 *  - getTotalHT / getTotalTVA : cas de base + gestion des nulls + arrondis.
 *  - getPurchase / getAllPurchases / getPurchasePending : mapping liste/élément.
 *  - getPurchasesByPurchaser / getPurchasesByPurchaserPending : validation existence utilisateur puis lecture dépôt.
 *  - setPurchase :
 *      * met à jour un achat non confirmé,
 *      * interdit la modif si achat confirmé,
 *      * interdit la modif si facture liée confirmée,
 *      * recalcule les totaux de facture si un lien est posé.
 *
 * Remarques :
 *  - Usage exclusif de Mockito, aucun contexte Spring n’est démarré.
 *  - On soigne le cas de stubbing pour éviter le "PotentialStubbingProblem" :
 *    le produit retourné par resolveProduct possède bien une TVA avec un ID non nul.
 */
@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock private PurchaseRepository purchaseRepository;
    @Mock private ResolveService resolveService;
    @Mock private InvoiceService invoiceService;

    @InjectMocks private PurchaseService service;

    // ------------------------------------------------------------------------------------
    // savePurchase
    // ------------------------------------------------------------------------------------
    @Test
    @DisplayName("savePurchase : normalise les valeurs depuis le produit et le taux de TVA (création sans facture)")
    void savePurchase_normalise_depuis_produit_et_tva() {
        // Arrange
        Long productId = 5L;
        Long purchaserId = 9L;

        // Produit existant avec prix unitaire et TVA associée (ID non nul indispensable)
        Tva tva = new Tva();
        tva.setId(2L);
        tva.setDefaultRate(new BigDecimal("20.00"));

        Product product = new Product();
        product.setId(productId);
        product.setName("Stylo");
        product.setUnitPriceHT(new BigDecimal("3.50"));
        product.setTva(tva);

        User purchaser = new User();
        purchaser.setId(purchaserId);

        // DTO d'entrée : invoiceId doit être null (interdit à la création)
        PurchaseDto input = new PurchaseDto(
            null,           // id
            productId,      // productId
            null,           // name (sera normalisé avec product.name)
            new BigDecimal("4"), // quantité
            null,           // unitPriceHT (sera normalisé)
            null,           // totalHT (sera normalisé)
            null,           // tvaApplied (sera normalisé)
            null,           // totalTva (sera normalisé)
            null,           // invoiceId -> DOIT rester null à la création
            purchaserId,    // purchaserId
            null            // isConfirmed -> sera forcé à FALSE
        );

        // Stubs de résolution
        when(resolveService.resolveProduct(productId)).thenReturn(product);
        when(resolveService.resolveUser(purchaserId)).thenReturn(purchaser);
        when(resolveService.resolveTva(2L)).thenReturn(tva);

        // Sauvegarde : on renvoie l'entité avec un ID simulé
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(inv -> {
            Purchase p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });

        // Act
        PurchaseDto out = service.savePurchase(input);

        // Assert : valeurs normalisées attendues
        assertThat(out.id()).isEqualTo(100L);
        assertThat(out.productId()).isEqualTo(productId);
        assertThat(out.name()).isEqualTo("Stylo");
        assertThat(out.quantity()).isEqualByComparingTo("4");
        assertThat(out.unitPriceHT()).isEqualByComparingTo("3.50");
        assertThat(out.totalHT()).isEqualByComparingTo("14.00");     // 4 × 3.50
        assertThat(out.tvaApplied()).isEqualByComparingTo("20.00");
        assertThat(out.totalTva()).isEqualByComparingTo("2.80");     // 14.00 × 20% = 2.80
        assertThat(out.invoiceId()).isNull();
        assertThat(out.isConfirmed()).isFalse();

        // Vérifie les résolutions et la persistance
        verify(resolveService).resolveProduct(productId);
        verify(resolveService).resolveUser(purchaserId);
        verify(resolveService).resolveTva(2L);
        verify(purchaseRepository).save(any(Purchase.class));
        verifyNoMoreInteractions(resolveService, purchaseRepository);
        verifyNoInteractions(invoiceService);
    }

    @Test
    @DisplayName("savePurchase : lève IllegalStateException si invoiceId est fourni à la création")
    void savePurchase_interdit_invoiceId_a_la_creation() {
        PurchaseDto input = new PurchaseDto(
            null, 1L, null, BigDecimal.ONE, null, null, null, null,
            999L, // invoiceId présent -> interdit
            5L, null
        );

        assertThatThrownBy(() -> service.savePurchase(input))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("facture");

        verifyNoInteractions(resolveService, purchaseRepository, invoiceService);
    }

    // ------------------------------------------------------------------------------------
    // getTotalHT / getTotalTVA
    // ------------------------------------------------------------------------------------
    @Test
    @DisplayName("getTotalHT : multiplie quantité × PU, retourne ZERO si un paramètre est null")
    void getTotalHT_ok_et_gere_nulls() {
        assertThat(service.getTotalHT(new BigDecimal("3"), new BigDecimal("2.50")))
            .isEqualByComparingTo("7.50");
        assertThat(service.getTotalHT(null, new BigDecimal("2.50")))
            .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(service.getTotalHT(new BigDecimal("3"), null))
            .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getTotalTVA : calcule totalHT × taux/100 arrondi à 2 décimales (HALF_UP) et gère nulls")
    void getTotalTVA_ok_et_arrondi() {
        assertThat(service.getTotalTVA(new BigDecimal("10.00"), new BigDecimal("20.00")))
            .isEqualByComparingTo("2.00");
        assertThat(service.getTotalTVA(new BigDecimal("10.00"), new BigDecimal("5.50")))
            .isEqualByComparingTo("0.55");
        // Arrondi HALF_UP sur 1/3 de centime
        assertThat(service.getTotalTVA(new BigDecimal("1.00"), new BigDecimal("16.6667")))
            .isEqualByComparingTo("0.17");
        assertThat(service.getTotalTVA(null, new BigDecimal("20.00")))
            .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(service.getTotalTVA(new BigDecimal("10.00"), null))
            .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ------------------------------------------------------------------------------------
    // getPurchase / getAllPurchases / getPurchasePending
    // ------------------------------------------------------------------------------------
    @Test
    @DisplayName("getPurchase : retourne un DTO quand l'achat existe, sinon null")
    void getPurchase_retourne_dto_ou_null() {
        Purchase entity = new Purchase();
        entity.setId(7L);
        when(purchaseRepository.findById(7L)).thenReturn(Optional.of(entity));
        when(purchaseRepository.findById(404L)).thenReturn(Optional.empty());

        assertThat(service.getPurchase(7L)).isNotNull();
        assertThat(service.getPurchase(404L)).isNull();

        verify(purchaseRepository).findById(7L);
        verify(purchaseRepository).findById(404L);
    }

    @Test
    @DisplayName("getAllPurchases : mappe la liste des entités en DTO")
    void getAllPurchases_ok() {
        Purchase p1 = new Purchase(); p1.setId(1L);
        Purchase p2 = new Purchase(); p2.setId(2L);
        when(purchaseRepository.findAll()).thenReturn(List.of(p1, p2));

        var list = service.getAllPurchases();

        assertThat(list).hasSize(2);
        assertThat(list.get(0).id()).isEqualTo(1L);
        assertThat(list.get(1).id()).isEqualTo(2L);
        verify(purchaseRepository).findAll();
    }

    @Test
    @DisplayName("getPurchasePending : mappe la liste des achats non confirmés")
    void getPurchasePending_ok() {
        Purchase p1 = new Purchase(); p1.setId(1L); p1.setConfirmed(false);
        Purchase p2 = new Purchase(); p2.setId(2L); p2.setConfirmed(false);
        when(purchaseRepository.findByIsConfirmedFalse()).thenReturn(List.of(p1, p2));

        var list = service.getPurchasePending();

        assertThat(list).hasSize(2);
        assertThat(list.get(0).id()).isEqualTo(1L);
        assertThat(list.get(1).id()).isEqualTo(2L);
        verify(purchaseRepository).findByIsConfirmedFalse();
    }

    // ------------------------------------------------------------------------------------
    // getPurchasesByPurchaser / getPurchasesByPurchaserPending
    // ------------------------------------------------------------------------------------
    @Test
    @DisplayName("getPurchasesByPurchaser : valide l'utilisateur puis retourne les achats")
    void getPurchasesByPurchaser_ok() {
        Long purchaserId = 55L;
        when(resolveService.resolveUser(purchaserId)).thenReturn(new User());
        Purchase p = new Purchase(); p.setId(3L);
        when(purchaseRepository.findByPurchaserId(purchaserId)).thenReturn(List.of(p));

        var list = service.getPurchasesByPurchaser(purchaserId);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).id()).isEqualTo(3L);

        verify(resolveService).resolveUser(purchaserId);
        verify(purchaseRepository).findByPurchaserId(purchaserId);
    }

    @Test
    @DisplayName("getPurchasesByPurchaser : lève EntityNotFoundException si l'utilisateur n'existe pas")
    void getPurchasesByPurchaser_throw_si_user_inexistant() {
        Long purchaserId = 77L;
        when(resolveService.resolveUser(purchaserId)).thenThrow(new EntityNotFoundException("not found"));

        assertThatThrownBy(() -> service.getPurchasesByPurchaser(purchaserId))
            .isInstanceOf(EntityNotFoundException.class);

        verify(resolveService).resolveUser(purchaserId);
        verifyNoInteractions(purchaseRepository);
    }

    @Test
    @DisplayName("getPurchasesByPurchaserPending : valide l'utilisateur puis retourne les achats non confirmés")
    void getPurchasesByPurchaserPending_ok() {
        Long purchaserId = 66L;
        when(resolveService.resolveUser(purchaserId)).thenReturn(new User());
        Purchase p = new Purchase(); p.setId(9L); p.setConfirmed(false);
        when(purchaseRepository.findByPurchaserIdAndIsConfirmedFalse(purchaserId)).thenReturn(List.of(p));

        var list = service.getPurchasesByPurchaserPending(purchaserId);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).id()).isEqualTo(9L);

        verify(resolveService).resolveUser(purchaserId);
        verify(purchaseRepository).findByPurchaserIdAndIsConfirmedFalse(purchaserId);
    }

    // ------------------------------------------------------------------------------------
    // setPurchase
    // ------------------------------------------------------------------------------------
    @Test
    @DisplayName("setPurchase : met à jour un achat non confirmé, lie éventuellement une facture et recalcule ses totaux")
    void setPurchase_happy_path() {
        // Entité existante non confirmée
        Purchase existing = new Purchase();
        existing.setId(10L);
        existing.setConfirmed(false);

        // Repository : retrouve l'achat et le sauvegarde
        when(purchaseRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(inv -> inv.getArgument(0));

        // On simule une facture non confirmée à lier
        Invoice inv = new Invoice();
        inv.setId(200L);
        inv.setConfirmed(false);
        when(resolveService.resolveInvoice(200L)).thenReturn(inv);
        when(invoiceService.updateInvoiceTotals(inv)).thenReturn(inv);

        // On simule la résolution d'un acheteur et d'un produit optionnels
        User purchaser = new User(); purchaser.setId(5L);
        when(resolveService.resolveUser(5L)).thenReturn(purchaser);

        Product product = new Product(); product.setId(3L);
        when(resolveService.resolveProduct(3L)).thenReturn(product);

        // DTO de mise à jour : changer produit, acheteur, lier facture, marquer confirmé
        PurchaseDto update = new PurchaseDto(
            10L,
            3L,                 // productId -> sera appliqué
            "Nouveau nom",      // name -> sera copié
            new BigDecimal("2.5"), // quantity -> sera copié
            new BigDecimal("9.99"), // unitPriceHT -> sera copié
            new BigDecimal("24.98"),// totalHT -> sera copié
            new BigDecimal("20.00"),// tvaApplied -> sera copié
            new BigDecimal("5.00"), // totalTva -> sera copié
            200L,               // invoiceId -> lien + recalcul totaux
            5L,                 // purchaserId -> sera appliqué
            Boolean.TRUE        // isConfirmed -> sera appliqué
        );

        // Act
        PurchaseDto out = service.setPurchase(update);

        // Assert : l'entité a été enrichie avec les résolutions et les champs non nuls
        assertThat(out.id()).isEqualTo(10L);
        assertThat(out.invoiceId()).isEqualTo(200L);
        assertThat(out.productId()).isEqualTo(3L);
        assertThat(out.purchaserId()).isEqualTo(5L);
        assertThat(out.isConfirmed()).isTrue();
        assertThat(out.name()).isEqualTo("Nouveau nom");
        assertThat(out.quantity()).isEqualByComparingTo("2.5");
        assertThat(out.unitPriceHT()).isEqualByComparingTo("9.99");

        // Vérifications d’interactions
        verify(purchaseRepository).findById(10L);
        verify(resolveService).resolveInvoice(200L);
        verify(invoiceService).updateInvoiceTotals(inv);
        verify(resolveService).resolveProduct(3L);
        verify(resolveService).resolveUser(5L);
        verify(purchaseRepository).save(any(Purchase.class));
    }

    @Test
    @DisplayName("setPurchase : lève IllegalStateException si l'achat est déjà confirmé")
    void setPurchase_throw_si_achat_confirme() {
        Purchase existing = new Purchase();
        existing.setId(11L);
        existing.setConfirmed(true);

        when(purchaseRepository.findById(11L)).thenReturn(Optional.of(existing));

        PurchaseDto update = new PurchaseDto(11L, null, null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.setPurchase(update))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("confirmés");

        verify(purchaseRepository).findById(11L);
        verifyNoMoreInteractions(purchaseRepository);
        verifyNoInteractions(resolveService, invoiceService);
    }

    @Test
    @DisplayName("setPurchase : lève IllegalStateException si la facture liée est confirmée")
    void setPurchase_throw_si_facture_confirmee() {
        Purchase existing = new Purchase();
        existing.setId(12L);
        existing.setConfirmed(false);
        when(purchaseRepository.findById(12L)).thenReturn(Optional.of(existing));

        Invoice inv = new Invoice();
        inv.setId(300L);
        inv.setConfirmed(true);
        when(resolveService.resolveInvoice(300L)).thenReturn(inv);

        PurchaseDto update = new PurchaseDto(12L, null, null, null, null, null, null, null, 300L, null, null);

        assertThatThrownBy(() -> service.setPurchase(update))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("confirmés"); // même message dans le service

        verify(purchaseRepository).findById(12L);
        verify(resolveService).resolveInvoice(300L);
        verifyNoMoreInteractions(purchaseRepository, resolveService);
        verifyNoInteractions(invoiceService);
    }

    @Test
    @DisplayName("setPurchase : lève EntityNotFoundException quand l'achat n'existe pas")
    void setPurchase_throw_si_achat_introuvable() {
        when(purchaseRepository.findById(999L)).thenReturn(Optional.empty());

        PurchaseDto update = new PurchaseDto(999L, null, null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.setPurchase(update))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("999");

        verify(purchaseRepository).findById(999L);
        verifyNoInteractions(resolveService, invoiceService);
    }
}
