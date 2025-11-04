package backend.services.product;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import backend.dtos.ProductDto;
import backend.models.Product;
import backend.models.Tva;

@Service
public class ProductMapperService {

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

    public static Product toEntity(ProductDto productDto, Tva tva) {
        Product product = new Product();
        product.setId(productDto.id());
        product.setName(productDto.name());
        product.setUnitPriceHT(productDto.unitPriceHT());
        product.setTva(tva);
        return product;
    }

    public static List<ProductDto> toDtoList(List<Product> products) {
        List<ProductDto> productDtos = new ArrayList<>();
        for (Product product : products) {
            productDtos.add(toDto(product));
        }
        return productDtos;
    }
}