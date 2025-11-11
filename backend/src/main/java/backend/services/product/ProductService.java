package backend.services.product;

import java.util.List;

import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;

import backend.dtos.ProductDto;
import backend.models.Product;
import backend.models.Tva;
import backend.repositories.ProductRepository;
import backend.services.ResolveService;
import backend.utilities.BeanCopyUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Service applicatif responsable de la gestion des produits ({@link Product}).
 *
 * <p>Ce service centralise la logique métier associée aux produits, incluant :</p>
 * <ul>
 *   <li>la création d’un nouveau produit,</li>
 *   <li>la récupération et la mise à jour d’un produit existant,</li>
 *   <li>et la conversion entre entités et DTOs via {@link ProductMapperService}.</li>
 * </ul>
 *
 * <p>Annoté avec {@link Service} pour être géré par Spring, et
 * {@link RequiredArgsConstructor} pour l’injection automatique des dépendances.</p>
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    /** Dépôt JPA pour la gestion des entités {@link Product}. */
    private final ProductRepository productRepository;

    /** Service utilitaire pour la résolution des entités liées (comme {@link Tva}). */
    private final ResolveService resolveService;

    /**
     * Crée et enregistre un nouveau produit à partir des informations du DTO fourni.
     *
     * <p>Les valeurs sont validées avant enregistrement : le nom, le prix unitaire HT
     * et l’identifiant de la TVA doivent être présents, sinon une {@link BadRequestException} est levée.</p>
     *
     * @param productDto les informations du produit à créer
     * @return le produit créé sous forme de {@link ProductDto}
     * @throws BadRequestException si des champs obligatoires sont manquants ou invalides
     * @throws jakarta.persistence.EntityNotFoundException si la TVA associée n’existe pas
     */
    public ProductDto saveProductDetails(ProductDto productDto) throws BadRequestException {
        Tva tva = resolveService.resolveTva(productDto.tvaId());
        ProductDto productUpdated = new ProductDto(
            null,
            productDto.name(),
            productDto.unitPriceHT(),
            productDto.tvaId()
        );

        if (productUpdated.name() == null || productUpdated.unitPriceHT() == null || productUpdated.tvaId() == null) {
            throw new BadRequestException("Veuillez entrer des valeurs valides");
        }

        Product productEntity = ProductMapperService.toEntity(productUpdated, tva);
        Product savedProduct = productRepository.save(productEntity);
        return ProductMapperService.toDto(savedProduct);
    }

    /**
     * Récupère un produit par son identifiant.
     *
     * @param id l’identifiant du produit à rechercher
     * @return le produit correspondant sous forme de {@link ProductDto}, ou {@code null} s’il n’existe pas
     */
    public ProductDto getProductDetails(Long id) {
        return productRepository.findById(id)
            .map(ProductMapperService::toDto)
            .orElse(null);
    }

    /**
     * Récupère la liste de tous les produits enregistrés.
     *
     * @return une liste de {@link ProductDto} représentant tous les produits
     */
    public List<ProductDto> getAllProductDetails() {
        List<Product> products = productRepository.findAll();
        return ProductMapperService.toDtoList(products);
    }

    /**
     * Met à jour un produit existant à partir d’un {@link ProductDto}.
     *
     * <p>Les propriétés non nulles du DTO sont copiées vers l’entité persistée
     * grâce à {@link BeanCopyUtils#copyNonNullProperties(Object, Object)}.
     * Si un identifiant de TVA est fourni, la relation est mise à jour.</p>
     *
     * @param productDetails les nouvelles informations du produit
     * @return le produit mis à jour sous forme de {@link ProductDto}
     * @throws EntityNotFoundException si le produit à mettre à jour est introuvable
     */
    public ProductDto setProductDetails(ProductDto productDetails) {
        Product updatedProduct = productRepository.findById(productDetails.id())
            .map(product -> {
                // Copie des propriétés non nulles du DTO vers l’entité
                BeanCopyUtils.copyNonNullProperties(productDetails, product);

                // Mise à jour de la relation TVA si un identifiant est présent
                if (productDetails.tvaId() != null) {
                    Tva tva = resolveService.resolveTva(productDetails.tvaId());
                    product.setTva(tva);
                }

                return productRepository.save(product);
            })
            .orElseThrow(() -> new EntityNotFoundException("Product not found with id " + productDetails.id()));

        return updatedProduct != null ? ProductMapperService.toDto(updatedProduct) : null;
    }
}
