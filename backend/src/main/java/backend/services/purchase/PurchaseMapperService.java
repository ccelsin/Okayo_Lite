package backend.services.purchase;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import backend.dtos.PurchaseDto;
import backend.models.Invoice;
import backend.models.Product;
import backend.models.Purchase;
import backend.models.User;

@Service
public class PurchaseMapperService {

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

    public static List<PurchaseDto> toDtoList(List<Purchase> purchases) {
        List<PurchaseDto> purchaseDtos = new ArrayList<>();
        for (Purchase purchase : purchases) {
            purchaseDtos.add(toDto(purchase));
        }
        return purchaseDtos;
    }
}