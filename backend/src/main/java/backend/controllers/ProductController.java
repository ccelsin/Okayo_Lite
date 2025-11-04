package backend.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import backend.dtos.ProductDto;
import backend.services.product.ProductService;
import backend.services.user.UserService;
import backend.utilities.ResponseUtils;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;
    private final UserService userService;

    // Create a product when the user has admin rights.
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<?> saveProduct(HttpServletRequest request, @RequestBody ProductDto productDto) {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.forbidden("Only admins can create product.");
        }
        ProductDto savedProduct = productService.saveProductDetails(productDto);
        return ResponseEntity.ok(savedProduct);
    }

    // List all products for an authorized user.
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<?> getAllProducts(HttpServletRequest request) {
        if (userService.isAuthorized(request) == false) {
            return ResponseUtils.unauthorized("Access denied");
        }
        List<ProductDto> products = productService.getAllProductDetails();
        return ResponseEntity.ok(products);
    }

    // Load one product by its id.
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        ProductDto product = productService.getProductDetails(id);
        if (product == null) {
            return ResponseUtils.notFound("Product not found with id " + id);
        }
        return ResponseEntity.ok(product);
    }

    // Update an existing product when the user has admin rights.
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public ResponseEntity<?> setProduct(HttpServletRequest request, @RequestBody ProductDto productDto) {
        if (userService.isAuthorized(request) == false) {
            return ResponseUtils.unauthorized("Access denied");
        }

        ProductDto updatedProduct = productService.setProductDetails(productDto);
        if (updatedProduct == null) {
            return ResponseUtils.badRequest("This product doesn't exist");
        }
        return ResponseEntity.ok(updatedProduct);
    }
}