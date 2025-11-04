package backend.services.purchase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;

import backend.dtos.ProductDto;
import backend.dtos.PurchaseDto;
import backend.models.Invoice;
import backend.models.Product;
import backend.models.Purchase;
import backend.models.Tva;
import backend.models.User;
import backend.repositories.PurchaseRepository;
import backend.services.ResolveService;
import backend.services.invoice.InvoiceService;
import backend.services.product.ProductMapperService;
import backend.utilities.BeanCopyUtils;
import backend.utilities.ResponseUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final ResolveService resolveService;
    private final InvoiceService invoiceService;


    public PurchaseDto savePurchase(PurchaseDto purchaseDto) {
        if (purchaseDto.invoiceId() != null) {
            throw new IllegalStateException("Invoice must be assigned by an administrator after purchase creation");
        }

        Product product = resolveService.resolveProduct(purchaseDto.productId());
        Invoice invoice = null;
        User purchaser = resolveService.resolveUser(purchaseDto.purchaserId());
        ProductDto productDto = ProductMapperService.toDto(product);
        Tva tva = resolveService.resolveTva(productDto.tvaId());

        PurchaseDto normalizedDto = new PurchaseDto(
            null,
            purchaseDto.productId(),
            product.getName(),
            purchaseDto.quantity(),
            product.getUnitPriceHT(),
            getTotalHT(purchaseDto.quantity(),product.getUnitPriceHT()),
            tva.getDefaultRate(),
            getTotalTVA(getTotalHT(purchaseDto.quantity(),product.getUnitPriceHT()),tva.getDefaultRate()),
            invoice != null ? invoice.getId() : null,
            purchaseDto.purchaserId(),
            Boolean.FALSE
        );

        Purchase purchaseEntity = PurchaseMapperService.toEntity(normalizedDto, product, invoice, purchaser);
        Purchase savedPurchase = purchaseRepository.save(purchaseEntity);
        return PurchaseMapperService.toDto(savedPurchase);
    }

    public PurchaseDto getPurchase(Long id) {
        return purchaseRepository.findById(id)
            .map(PurchaseMapperService::toDto)
            .orElse(null);
    }

    public BigDecimal getTotalHT(BigDecimal quantity, BigDecimal unitPriceHT){
        if (quantity == null || unitPriceHT == null) {
            return BigDecimal.ZERO;
        }
        return quantity.multiply(unitPriceHT);  
    }

    public BigDecimal getTotalTVA(BigDecimal totalHT, BigDecimal tvaRate) {
        if (totalHT == null || tvaRate == null) {
            return BigDecimal.ZERO;
        }

        return totalHT
                .multiply(tvaRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }


    public List<PurchaseDto> getAllPurchases() {
        List<Purchase> purchases = purchaseRepository.findAll();
        return PurchaseMapperService.toDtoList(purchases);
    }

    public List<PurchaseDto> getPurchasePending(){
        List<Purchase> purchases = purchaseRepository.findByIsConfirmedFalse();
        return PurchaseMapperService.toDtoList(purchases);
    }

    public List<PurchaseDto> getPurchasesByPurchaser(Long purchaserId) {
        // Ensure purchaser exists and is not an admin; resolveService.resolveUser will throw if invalid
        resolveService.resolveUser(purchaserId);
        List<Purchase> purchases = purchaseRepository.findByPurchaserId(purchaserId);

        return PurchaseMapperService.toDtoList(purchases);
    }

    public List<PurchaseDto> getPurchasesByPurchaserPending(Long purchaserId) {
        // Ensure purchaser exists and is not an admin; resolveService.resolveUser will throw if invalid
        resolveService.resolveUser(purchaserId);
        List<Purchase> purchases = purchaseRepository.findByPurchaserIdAndIsConfirmedFalse(purchaserId);

        return PurchaseMapperService.toDtoList(purchases);
    }

    public PurchaseDto setPurchase(PurchaseDto purchaseDto) {
        Purchase purchase = purchaseRepository.findById(purchaseDto.id())
            .orElseThrow(() -> new EntityNotFoundException("Purchase not found with id " + purchaseDto.id()));

        if (purchase.isConfirmed()) {
            throw new IllegalStateException("Confirmed purchases cannot be modified");
        }

        Product product = purchaseDto.productId() != null ? resolveService.resolveProduct(purchaseDto.productId()) : null;
        Invoice invoice = purchaseDto.invoiceId() != null ? resolveService.resolveInvoice(purchaseDto.invoiceId()) : null;
        User purchaser = purchaseDto.purchaserId() != null ? resolveService.resolveUser(purchaseDto.purchaserId()) : null;
        if (invoice!= null && invoice.isConfirmed()) {
            throw new IllegalStateException("Confirmed purchases cannot be modified");
        }
        purchase.setInvoice(invoice);

        if(invoice != null){
            invoice = invoiceService.updateInvoiceTotals(invoice);
        }

        
        BeanCopyUtils.copyNonNullProperties(purchaseDto, purchase);

        if (product != null) {
            purchase.setProduct(product);
        }
        if (purchaseDto.isConfirmed() != null) {
            purchase.setConfirmed(purchaseDto.isConfirmed());
        }
        
        if (purchaser != null) {
            purchase.setPurchaser(purchaser);
        }

        Purchase savedPurchase = purchaseRepository.save(purchase);
        return PurchaseMapperService.toDto(savedPurchase);
    }

}