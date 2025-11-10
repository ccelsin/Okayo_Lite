package backend.services.invoice;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import backend.dtos.InvoiceDto;
import backend.models.Invoice;
import backend.models.PaymentDetails;
import backend.models.Purchase;
import backend.models.User;

/**
 * Service utilitaire permettant de convertir les entités {@link Invoice}
 * en objets de transfert de données {@link InvoiceDto} et inversement.
 *
 * <p>Ce mapper facilite la communication entre la couche de persistance
 * et la couche de présentation (ou de service) en isolant la logique
 * de transformation des données.</p>
 *
 * <p>Annoté avec {@link Service} afin d’être reconnu comme composant Spring,
 * même si ses méthodes sont statiques.</p>
 */
@Service
public class InvoiceMapperService {

    /**
     * Convertit une entité {@link Invoice} en un objet {@link InvoiceDto}.
     *
     * <p>Cette méthode extrait uniquement les identifiants des entités associées
     * (client, créateur, détails de paiement, achats) afin de limiter la taille
     * et la complexité du DTO retourné.</p>
     *
     * @param invoice l’entité {@link Invoice} à convertir
     * @return un {@link InvoiceDto} contenant les données principales de la facture
     */
    public static InvoiceDto toDto(Invoice invoice) {
        Long customerId = invoice.getCustomer() != null ? invoice.getCustomer().getId() : null;
        Long creatorId = invoice.getCreator() != null ? invoice.getCreator().getId() : null;
        Long paymentDetailsId = invoice.getPaymentDetails() != null ? invoice.getPaymentDetails().getId() : null;
        Boolean isConfirmed = Boolean.valueOf(invoice.isConfirmed());

        List<Long> purchaseIds = new ArrayList<>();
        if (invoice.getPurchases() != null) {
            for (Purchase purchase : invoice.getPurchases()) {
                if (purchase != null && purchase.getId() != null) {
                    purchaseIds.add(purchase.getId());
                }
            }
        }

        return new InvoiceDto(
            invoice.getId(),
            invoice.getReference(),
            invoice.getBillingDate(),
            invoice.getDueDate(),
            invoice.getTotalHT(),
            invoice.getTotalTTC(),
            customerId,
            creatorId,
            paymentDetailsId,
            purchaseIds,
            isConfirmed
        );
    }

    /**
     * Convertit un objet {@link InvoiceDto} en une entité {@link Invoice}.
     *
     * <p>Cette méthode permet de reconstruire une entité complète en injectant
     * les relations nécessaires (client, créateur, paiements et achats).</p>
     *
     * @param invoiceDto le DTO contenant les informations de la facture
     * @param customer le client associé à la facture
     * @param creator l’utilisateur ayant créé la facture
     * @param paymentDetails les informations de paiement associées
     * @param purchases la liste des achats rattachés à la facture
     * @return une entité {@link Invoice} prête à être persistée
     */
    public static Invoice toEntity(InvoiceDto invoiceDto, User customer, User creator, PaymentDetails paymentDetails, List<Purchase> purchases) {
        Invoice invoice = new Invoice();
        invoice.setId(invoiceDto.id());
        invoice.setBillingDate(invoiceDto.billingDate());
        invoice.setDueDate(invoiceDto.dueDate());
        invoice.setTotalHT(invoiceDto.totalHT());
        invoice.setTotalTTC(invoiceDto.totalTTC());
        invoice.setCustomer(customer);
        invoice.setCreator(creator);
        invoice.setPaymentDetails(paymentDetails);
        invoice.setPurchases(purchases);
        return invoice;
    }

    /**
     * Convertit une liste d’entités {@link Invoice} en une liste de {@link InvoiceDto}.
     *
     * <p>Chaque facture est transformée individuellement via la méthode {@link #toDto(Invoice)}.</p>
     *
     * @param invoices la liste d’entités à convertir
     * @return la liste correspondante de DTOs
     */
    public static List<InvoiceDto> toDtoList(List<Invoice> invoices) {
        List<InvoiceDto> invoiceDtos = new ArrayList<>();
        for (Invoice invoice : invoices) {
            invoiceDtos.add(toDto(invoice));
        }
        return invoiceDtos;
    }
}
