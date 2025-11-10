package backend.services.invoice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

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
import backend.utilities.BeanCopyUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Service applicatif de gestion des factures ({@link Invoice}).
 *
 * <p>Responsabilités :</p>
 * <ul>
 *   <li>Création d'une facture (génération de référence, initialisation des totaux),</li>
 *   <li>Calcul des totaux HT/TTC depuis les achats,</li>
 *   <li>Mise à jour des totaux et confirmation des achats lors de la validation,</li>
 *   <li>Consultation des factures (toutes, par client, par id).</li>
 * </ul>
 *
 * <p>Annotations :</p>
 * <ul>
 *   <li>{@link Service} : composant Spring géré par le conteneur,</li>
 *   <li>{@link RequiredArgsConstructor} : injection par constructeur des dépendances {@code final}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class InvoiceService {

    /** Dépôt JPA pour l'accès aux entités {@link Invoice}. */
    private final InvoiceRepository invoiceRepository;

    /** Service utilitaire pour résoudre les entités liées (utilisateur, paiement, etc.). */
    private final ResolveService resolveService;

    /** Dépôt JPA pour l'accès aux entités {@link Purchase}. */
    private final PurchaseRepository purchaseRepository;

    /**
     * Crée et enregistre une facture à partir d'une requête.
     *
     * <p>Initialise la référence, les dates, le client, l'émetteur et les
     * détails de paiement. Les totaux sont initialisés à zéro.</p>
     *
     * @param creatorId identifiant du créateur de la facture
     * @param invoiceRequest données de création (dates, client, paymentDetailsId)
     * @return la facture créée au format {@link InvoiceDto}
     * @throws jakarta.persistence.EntityNotFoundException si le créateur, le client ou les détails de paiement sont introuvables
     */
    public InvoiceDto saveInvoice(Long creatorId, InvoiceRequest invoiceRequest) {
        User creator = resolveService.resolveUser(creatorId);
        PaymentDetails paymentDetails = resolveService.resolvePaymentDetails(invoiceRequest.paymentDetailsId());

        Invoice invoice = new Invoice();
        invoice.setReference(generateReference());
        invoice.setBillingDate(invoiceRequest.billingDate());
        invoice.setCustomer(resolveService.resolveUser(invoiceRequest.customerId()));
        invoice.setDueDate(invoiceRequest.dueDate());
        invoice.setCreator(creator);
        invoice.setPaymentDetails(paymentDetails);

        // Initialisation des totaux
        invoice.setTotalHT(BigDecimal.ZERO);
        invoice.setTotalTTC(BigDecimal.ZERO);

        Invoice savedInvoice = invoiceRepository.save(invoice);
        return InvoiceMapperService.toDto(savedInvoice);
    }

    /**
     * Calcule le total HT de la facture à partir des achats liés.
     *
     * @param invoice facture source
     * @return total HT (arrondi à 2 décimales, HALF_UP) ou 0 si aucun achat
     */
    public BigDecimal calculateTotalHT(Invoice invoice) {
        if (invoice.getPurchases() == null || invoice.getPurchases().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return invoice.getPurchases().stream()
                .map(Purchase::getTotalHT)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcule le total TTC de la facture (somme de HT + TVA pour chaque achat).
     *
     * @param invoice facture source
     * @return total TTC (arrondi à 2 décimales, HALF_UP) ou 0 si aucun achat
     */
    public BigDecimal calculateTotalTTC(Invoice invoice) {
        if (invoice.getPurchases() == null || invoice.getPurchases().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return invoice.getPurchases().stream()
                .map(p -> {
                    if (p.getTotalHT() == null || p.getTotalTva() == null) {
                        return BigDecimal.ZERO;
                    }
                    return p.getTotalHT().add(p.getTotalTva());
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Recalcule et persiste les totaux HT/TTC d'une facture.
     *
     * @param invoice facture à mettre à jour
     * @return la facture persistée avec les totaux recalculés
     */
    // Update the invoice totals after adding purchase
    public Invoice updateInvoiceTotals(Invoice invoice) {
        BigDecimal totalHT = calculateTotalHT(invoice);
        BigDecimal totalTTC = calculateTotalTTC(invoice);

        invoice.setTotalHT(totalHT);
        invoice.setTotalTTC(totalTTC);

        return invoiceRepository.save(invoice);
    }

    /**
     * Récupère une facture par son identifiant.
     *
     * @param id identifiant de la facture
     * @return {@link InvoiceDto} si trouvée, sinon {@code null}
     */
    public InvoiceDto getInvoice(Long id) {
        return invoiceRepository.findById(id)
                .map(InvoiceMapperService::toDto)
                .orElse(null);
    }

    /**
     * Récupère toutes les factures en base et met à jour les totaux si nécessaire.
     *
     * <p><strong>Note d'implémentation :</strong> la boucle met à jour les totaux
     * pour les factures non confirmées, puis appelle à nouveau {@code updateInvoiceTotals(invoice)}
     * pour chaque facture (y compris confirmée). Cela provoque un double appel possible
     * sur les non confirmées. Gardé tel quel (pas de modification de code).</p>
     *
     * @return liste des factures au format {@link InvoiceDto}
     */
    public List<InvoiceDto> getAllInvoices() {
        List<Invoice> invoices = invoiceRepository.findAll();

        // Met à jour les factures non confirmées
        for (Invoice invoice : invoices) {
        if(invoice.isConfirmed() == false){
            updateInvoiceTotals(invoice);
        }
        updateInvoiceTotals(invoice);
    }

        // Convertit en DTOs
        return InvoiceMapperService.toDtoList(invoices);
    }

    /**
     * Récupère toutes les factures confirmées d'un client.
     *
     * @param id identifiant du client
     * @return liste des factures confirmées de ce client
     */
    public List<InvoiceDto> getInvoicesOfCustomer(Long id) {
        List<Invoice> invoices = invoiceRepository.findByCustomerIdAndIsConfirmedTrue(id);
        return InvoiceMapperService.toDtoList(invoices);
    }

    /**
     * Met à jour les propriétés d'une facture (dates, confirmation).
     *
     * <p>En cas de confirmation, tous les achats rattachés sont confirmés et persistés.
     * Les totaux sont recalculés avant la sauvegarde.</p>
     *
     * @param id identifiant de la facture à modifier
     * @param invoiceUpdateRequest données de mise à jour (dates, confirmation)
     * @return la facture mise à jour au format {@link InvoiceDto}
     * @throws EntityNotFoundException si la facture est introuvable
     */
    public InvoiceDto setInvoice(Long id, InvoiceUpdateRequest invoiceUpdateRequest) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found with id " + id));
        if (invoiceUpdateRequest.isConfirmed()) {
            // Confirme chaque achat
            if (invoice.getPurchases() != null && !invoice.getPurchases().isEmpty()) {
                invoice.getPurchases().forEach(p -> p.setConfirmed(true));
                // Enregiste les achats
                purchaseRepository.saveAll(invoice.getPurchases());
                BeanCopyUtils.copyNonNullProperties(invoiceUpdateRequest, invoice);
                Invoice savedInvoice = invoiceRepository.save(invoice);
                return InvoiceMapperService.toDto(savedInvoice);
            }
        }
        if (invoiceUpdateRequest.billingDate() != null) {
            invoice.setBillingDate(invoiceUpdateRequest.billingDate());
        }
        if (invoiceUpdateRequest.dueDate() != null) {
            invoice.setDueDate(invoiceUpdateRequest.dueDate());
        }

        // Recalcule les totaux
        updateInvoiceTotals(invoice);

        Invoice savedInvoice = invoiceRepository.save(invoice);
        return InvoiceMapperService.toDto(savedInvoice);
    }

    /**
     * Génère une référence pseudo-aléatoire de facture sur le format {@code 0000-0000}.
     *
     * <p><strong>Note d'implémentation :</strong> en cas de collision trouvée
     * dans le dépôt, la méthode rappelle {@code generateReference()} mais ne retourne
     * pas le résultat récursif. Comportement conservé tel quel (pas de modification minimale du code).</p>
     *
     * @return la référence générée
     */
    private String generateReference() {
        String preffix = String.format("%04d", ThreadLocalRandom.current().nextInt(0, 10000));
        String suffix = String.format("%04d", ThreadLocalRandom.current().nextInt(0, 10000));
        String reference = preffix + "-" + suffix;
        if(invoiceRepository.findByReference(reference).isPresent()) {
            generateReference();
        }
        return reference;
    }
}
