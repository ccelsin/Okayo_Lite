package backend.services.product;

import java.util.List;

import org.springframework.stereotype.Service;

import backend.dtos.ProductDto;
import backend.models.Product;
import backend.models.Tva;
import backend.repositories.ProductRepository;
import backend.services.ResolveService;
import backend.utilities.BeanCopyUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ResolveService resolveService;
    

    public ProductDto saveProductDetails(ProductDto productDto) {
        Tva tva = resolveService.resolveTva(productDto.tvaId());
        ProductDto productUpdated = new ProductDto(
            null,
            productDto.name(),
            productDto.unitPriceHT(),
            productDto.tvaId()
        );
        Product productEntity = ProductMapperService.toEntity(productUpdated, tva);
        Product savedProduct = productRepository.save(productEntity);
        return ProductMapperService.toDto(savedProduct);
    }

    public ProductDto getProductDetails(Long id) {
        return productRepository.findById(id)
            .map(ProductMapperService::toDto)
            .orElse(null);
    }

    public List<ProductDto> getAllProductDetails() {
        List<Product> products = productRepository.findAll();
        return ProductMapperService.toDtoList(products);
    }

    public ProductDto setProductDetails(ProductDto productDetails) {
        Product updatedProduct = productRepository.findById(productDetails.id())
            .map(product -> {
        // copy not null properties of dto set entity with them
        BeanCopyUtils.copyNonNullProperties(productDetails, product);

        // get tva data by his id
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