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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Service applicatif gérant le cycle de vie des achats ({@link Purchase}).
 *
 * <p>Responsabilités principales :</p>
 * <ul>
 *   <li>Création d’un achat à partir d’un {@link PurchaseDto} en normalisant les valeurs (prix HT, TVA, totaux),</li>
 *   <li>Consultation d’un achat et de collections d’achats (tous, en attente, par acheteur),</li>
 *   <li>Mise à jour d’un achat non confirmé, avec recalcul des totaux de facture si nécessaire.</li>
 * </ul>
 *
 * <p>Ce service s’appuie sur :</p>
 * <ul>
 *   <li>{@link PurchaseRepository} pour la persistance,</li>
 *   <li>{@link ResolveService} pour la résolution d’entités par identifiant (validation d’existence),</li>
 *   <li>{@link InvoiceService} pour la mise à jour des totaux de facture.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PurchaseService {

    /** Dépôt d’accès aux entités {@link Purchase}. */
    private final PurchaseRepository purchaseRepository;

    /** Service de résolution d’entités (produit, facture, utilisateur, tva, etc.). */
    private final ResolveService resolveService;

    /** Service métier facture (recalcul des totaux, etc.). */
    private final InvoiceService invoiceService;

    /**
     * Crée et enregistre un achat à partir d’un {@link PurchaseDto}.
     *
     * <p>Règles :</p>
     * <ul>
     *   <li>Un achat ne peut pas être directement rattaché à une facture lors de la création
     *       (lien possible après création) ; sinon {@link IllegalStateException}.</li>
     *   <li>Les valeurs financières (PU HT, Total HT, TVA appliquée, Total TVA) sont
     *       normalisées en fonction du produit et du taux de TVA par défaut.</li>
     * </ul>
     *
     * @param purchaseDto données d’entrée
     * @return l’achat créé en {@link PurchaseDto}
     * @throws IllegalStateException si une facture est fournie à la création
     * @throws jakarta.persistence.EntityNotFoundException via {@link ResolveService} si une entité liée est introuvable
     */
    public PurchaseDto savePurchase(PurchaseDto purchaseDto) {
        if (purchaseDto.invoiceId() != null) {
            throw new IllegalStateException("Une facture ne peut être assigné à un achat qu'après sa création");
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

    /**
     * Récupère un achat par identifiant.
     *
     * @param id identifiant de l’achat
     * @return {@link PurchaseDto} si trouvé, sinon {@code null}
     */
    public PurchaseDto getPurchase(Long id) {
        return purchaseRepository.findById(id)
            .map(PurchaseMapperService::toDto)
            .orElse(null);
    }

    /**
     * Calcule le total HT = quantité × prix unitaire HT.
     *
     * @param quantity quantité (peut être {@code null})
     * @param unitPriceHT prix unitaire HT (peut être {@code null})
     * @return total HT ou {@link BigDecimal#ZERO} si un des paramètres est nul
     */
    public BigDecimal getTotalHT(BigDecimal quantity, BigDecimal unitPriceHT){
        if (quantity == null || unitPriceHT == null) {
            return BigDecimal.ZERO;
        }
        return quantity.multiply(unitPriceHT);  
    }

    /**
     * Calcule le total TVA = totalHT × (tvaRate / 100), arrondi à 2 décimales (HALF_UP).
     *
     * @param totalHT total hors taxe
     * @param tvaRate taux de TVA en pourcentage (ex. 20 pour 20%)
     * @return total TVA ou {@link BigDecimal#ZERO} si un des paramètres est nul
     */
    public BigDecimal getTotalTVA(BigDecimal totalHT, BigDecimal tvaRate) {
        if (totalHT == null || tvaRate == null) {
            return BigDecimal.ZERO;
        }

        return totalHT
                .multiply(tvaRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * Récupère tous les achats.
     *
     * @return liste de {@link PurchaseDto}
     */
    public List<PurchaseDto> getAllPurchases() {
        List<Purchase> purchases = purchaseRepository.findAll();
        return PurchaseMapperService.toDtoList(purchases);
    }

    /**
     * Récupère tous les achats non confirmés.
     *
     * @return liste des achats en attente (non confirmés)
     */
    public List<PurchaseDto> getPurchasePending(){
        List<Purchase> purchases = purchaseRepository.findByIsConfirmedFalse();
        return PurchaseMapperService.toDtoList(purchases);
    }

    /**
     * Récupère tous les achats d’un acheteur donné.
     *
     * <p>Valide d’abord l’existence de l’utilisateur via {@link ResolveService#resolveUser(Long)}.</p>
     *
     * @param purchaserId identifiant de l’acheteur
     * @return liste des achats de cet utilisateur
     * @throws jakarta.persistence.EntityNotFoundException si l’utilisateur n’existe pas
     */
    public List<PurchaseDto> getPurchasesByPurchaser(Long purchaserId) {
        // Ensure purchaser exists and is not an admin; resolveService.resolveUser will throw if invalid
        resolveService.resolveUser(purchaserId);
        List<Purchase> purchases = purchaseRepository.findByPurchaserId(purchaserId);

        return PurchaseMapperService.toDtoList(purchases);
    }

    /**
     * Récupère tous les achats non confirmés d’un acheteur donné.
     *
     * <p>Valide d’abord l’existence de l’utilisateur via {@link ResolveService#resolveUser(Long)}.</p>
     *
     * @param purchaserId identifiant de l’acheteur
     * @return liste des achats non confirmés de cet utilisateur
     * @throws jakarta.persistence.EntityNotFoundException si l’utilisateur n’existe pas
     */
    public List<PurchaseDto> getPurchasesByPurchaserPending(Long purchaserId) {
        // Ensure purchaser exists and is not an admin; resolveService.resolveUser will throw if invalid
        resolveService.resolveUser(purchaserId);
        List<Purchase> purchases = purchaseRepository.findByPurchaserIdAndIsConfirmedFalse(purchaserId);

        return PurchaseMapperService.toDtoList(purchases);
    }

    /**
     * Met à jour un achat existant (uniquement s’il n’est pas confirmé).
     *
     * <p>Règles :</p>
     * <ul>
     *   <li>Si l’achat est confirmé → {@link IllegalStateException} (non modifiable).</li>
     *   <li>Si une facture associée est confirmée → {@link IllegalStateException} (non modifiable).</li>
     *   <li>Les relations (produit, acheteur, facture) sont résolues si des identifiants sont fournis.</li>
     *   <li>Les propriétés non nulles du DTO écrasent celles de l’entité via {@link BeanCopyUtils#copyNonNullProperties}.</li>
     *   <li>Si une facture est liée, ses totaux sont recalculés via {@link InvoiceService#updateInvoiceTotals(Invoice)}.</li>
     * </ul>
     *
     * @param purchaseDto données d’entrée (id requis)
     * @return l’achat mis à jour en {@link PurchaseDto}
     * @throws EntityNotFoundException si l’achat n’existe pas
     * @throws IllegalStateException si l’achat ou la facture liée est confirmée
     */
    public PurchaseDto setPurchase(PurchaseDto purchaseDto) {
        Purchase purchase = purchaseRepository.findById(purchaseDto.id())
            .orElseThrow(() -> new EntityNotFoundException("L'achat recherché n'a pas été retrouvé" + purchaseDto.id()));

        if (purchase.isConfirmed()) {
            throw new IllegalStateException("Les achats confirmés ne peuvent pas être modifié");
        }

        Product product = purchaseDto.productId() != null ? resolveService.resolveProduct(purchaseDto.productId()) : null;
        Invoice invoice = purchaseDto.invoiceId() != null ? resolveService.resolveInvoice(purchaseDto.invoiceId()) : null;
        User purchaser = purchaseDto.purchaserId() != null ? resolveService.resolveUser(purchaseDto.purchaserId()) : null;
        if (invoice!= null && invoice.isConfirmed()) {
            throw new IllegalStateException("Les achats confirmés ne peuvent pas être modifié");
        }
        purchase.setInvoice(invoice);

        if(invoice != null){
            invoice = invoiceService.updateInvoiceTotals(invoice);
        }

        // Copie des champs non nuls du DTO vers l’entité
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
