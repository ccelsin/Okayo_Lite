package backend.services.product;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import backend.dtos.ProductDto;
import backend.models.Product;
import backend.models.Tva;

/**
 * Service utilitaire de mappage entre les entités {@link Product}
 * et leurs représentations de transfert de données {@link ProductDto}.
 *
 * <p>Ce service permet d’assurer une séparation claire entre la couche de persistance
 * (entités JPA) et la couche de transfert (DTOs) utilisée dans les contrôleurs
 * ou services métiers. Il garantit la cohérence des conversions entre les deux formats.</p>
 *
 * <p>Annoté avec {@link Service} pour permettre son injection par Spring.</p>
 */
@Service
public class ProductMapperService {

    /**
     * Convertit une entité {@link Product} en un objet {@link ProductDto}.
     *
     * <p>Si une {@link Tva} est associée au produit, son identifiant est extrait ;
     * sinon, la valeur de l’identifiant de TVA est {@code null}.</p>
     *
     * @param product l’entité {@link Product} à convertir
     * @return un {@link ProductDto} représentant le produit
     */
    public static ProductDto toDto(Product product) {
        Tva tva = product.getTva();
        Long tvaId = tva != null ? tva.getId() : null;
        return new ProductDto(
            product.getId(),
            product.getName(),
            product.getUnitPriceHT(),
            tvaId
        );
    }

    /**
     * Convertit un objet {@link ProductDto} en une entité {@link Product}.
     *
     * <p>La relation vers {@link Tva} est injectée directement à partir
     * du paramètre passé à la méthode.</p>
     *
     * @param productDto le DTO à convertir
     * @param tva l’entité {@link Tva} associée (peut être {@code null})
     * @return une instance de {@link Product} initialisée à partir du DTO
     */
    public static Product toEntity(ProductDto productDto, Tva tva) {
        Product product = new Product();
        product.setId(productDto.id());
        product.setName(productDto.name());
        product.setUnitPriceHT(productDto.unitPriceHT());
        product.setTva(tva);
        return product;
    }

    /**
     * Convertit une liste d’entités {@link Product} en une liste de {@link ProductDto}.
     *
     * @param products la liste des entités produits
     * @return la liste correspondante des DTOs produits
     */
    public static List<ProductDto> toDtoList(List<Product> products) {
        List<ProductDto> productDtos = new ArrayList<>();
        for (Product product : products) {
            productDtos.add(toDto(product));
        }
        return productDtos;
    }
}
