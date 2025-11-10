package backend.services.purchase;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import backend.dtos.PurchaseDto;
import backend.models.Invoice;
import backend.models.Product;
import backend.models.Purchase;
import backend.models.User;

/**
 * Service utilitaire de mappage entre les entités {@link Purchase}
 * et leurs représentations de transfert de données {@link PurchaseDto}.
 *
 * <p>Ce service a pour but de centraliser la logique de transformation entre
 * la couche métier (entités JPA) et la couche de transfert (DTOs),
 * afin d’assurer la cohérence des conversions et d’éviter la duplication du code.</p>
 *
 * <p>Annoté avec {@link Service} pour permettre son injection par Spring
 * si nécessaire dans d’autres composants.</p>
 */
@Service
public class PurchaseMapperService {

    /**
     * Convertit une entité {@link Purchase} en un objet {@link PurchaseDto}.
     *
     * <p>Les identifiants des entités associées (produit, facture, acheteur)
     * sont extraits de manière sécurisée, même si les relations sont nulles.</p>
     *
     * @param purchase l’entité {@link Purchase} à convertir
     * @return un {@link PurchaseDto} contenant les informations de l’achat
     */
    public static PurchaseDto toDto(Purchase purchase) {
        Long productId = purchase.getProduct() != null ? purchase.getProduct().getId() : null;
        Long invoiceId = purchase.getInvoice() != null ? purchase.getInvoice().getId() : null;
        Long purchaserId = purchase.getPurchaser() != null ? purchase.getPurchaser().getId() : null;

        return new PurchaseDto(
            purchase.getId(),
            productId,
            purchase.getName(),
            purchase.getQuantity(),
            purchase.getUnitPriceHT(),
            purchase.getTotalHT(),
            purchase.getTvaApplied(),
            purchase.getTotalTva(),
            invoiceId,
            purchaserId,
            purchase.isConfirmed()
        );
    }

    /**
     * Convertit un {@link PurchaseDto} en une entité {@link Purchase},
     * tout en associant directement les entités liées fournies en paramètre.
     *
     * @param purchaseDto le DTO d’achat à convertir
     * @param product le produit associé (peut être {@code null})
     * @param invoice la facture associée (peut être {@code null})
     * @param purchaser l’acheteur associé (peut être {@code null})
     * @return une instance de {@link Purchase} remplie avec les données du DTO
     */
    public static Purchase toEntity(PurchaseDto purchaseDto, Product product, Invoice invoice, User purchaser) {
        Purchase purchase = new Purchase();
        purchase.setId(purchaseDto.id());
        purchase.setProduct(product);
        purchase.setName(purchaseDto.name());
        purchase.setQuantity(purchaseDto.quantity());
        purchase.setUnitPriceHT(purchaseDto.unitPriceHT());
        purchase.setTotalHT(purchaseDto.totalHT());
        purchase.setTvaApplied(purchaseDto.tvaApplied());
        purchase.setTotalTva(purchaseDto.totalTva());
        purchase.setInvoice(invoice);
        purchase.setPurchaser(purchaser);
        if (purchaseDto.isConfirmed() != null) {
            purchase.setConfirmed(purchaseDto.isConfirmed());
        }
        return purchase;
    }

    /**
     * Convertit une liste d’entités {@link Purchase} en une liste de {@link PurchaseDto}.
     *
     * @param purchases la liste des entités à convertir
     * @return la liste correspondante de {@link PurchaseDto}
     */
    public static List<PurchaseDto> toDtoList(List<Purchase> purchases) {
        List<PurchaseDto> purchaseDtos = new ArrayList<>();
        for (Purchase purchase : purchases) {
            purchaseDtos.add(toDto(purchase));
        }
        return purchaseDtos;
    }
}
