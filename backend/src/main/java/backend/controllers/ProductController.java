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

/**
 * Contrôleur REST responsable de la gestion des produits ({@link backend.models.Product}).
 *
 * <p>Ce contrôleur expose des endpoints pour :
 * <ul>
 *   <li>Créer un nouveau produit (réservé aux administrateurs),</li>
 *   <li>Afficher la liste de tous les produits,</li>
 *   <li>Afficher un produit spécifique par son identifiant,</li>
 *   <li>Mettre à jour un produit existant (réservé aux administrateurs).</li>
 * </ul>
 *
 * <p>Toutes les routes sont sécurisées et nécessitent un token JWT valide.
 * Les rôles sont vérifiés via le {@link UserService}.</p>
 *
 * <p>Les réponses HTTP sont standardisées grâce à la classe {@link ResponseUtils}.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product")
public class ProductController {

    /** Service métier gérant la logique de manipulation des produits. */
    private final ProductService productService;

    /** Service utilisateur permettant la vérification des autorisations et rôles. */
    private final UserService userService;

    /**
     * Crée un nouveau produit dans le système.
     *
     * <p>Accessible uniquement aux administrateurs. Si l’utilisateur connecté
     * n’a pas les droits nécessaires, une réponse HTTP 403 est renvoyée.</p>
     *
     * @param request la requête HTTP contenant le token JWT
     * @param productDto les informations du produit à enregistrer
     * @return le produit créé sous forme de {@link ProductDto}, ou une erreur d’accès
     * @throws Exception si les données envoyées ne sont pas valides
     */
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<?> saveProduct(HttpServletRequest request, @RequestBody ProductDto productDto) throws Exception {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.forbidden("Les administrateurs sont les seuls à pouvoir créer des produits");
        }
        ProductDto savedProduct = productService.saveProductDetails(productDto);
        return ResponseEntity.ok(savedProduct);
    }

    /**
     * Récupère la liste complète des produits enregistrés.
     *
     * <p>Accessible à tout utilisateur disposant d’un token JWT valide.
     * Si le token est manquant ou invalide, une réponse HTTP 401 est renvoyée.</p>
     *
     * @param request la requête HTTP contenant le token JWT
     * @return une liste de produits ({@link ProductDto})
     */
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<?> getAllProducts(HttpServletRequest request) {
        if (userService.isAuthorized(request) == false) {
            return ResponseUtils.unauthorized("Accès non autorisé");
        }
        List<ProductDto> products = productService.getAllProductDetails();
        return ResponseEntity.ok(products);
    }

    /**
     * Récupère un produit spécifique à partir de son identifiant.
     *
     * <p>Si aucun produit correspondant n’est trouvé, une réponse HTTP 404 est renvoyée.</p>
     *
     * @param id l’identifiant unique du produit
     * @return les détails du produit ou un message d’erreur
     */
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        ProductDto product = productService.getProductDetails(id);
        if (product == null) {
            return ResponseUtils.notFound("Produit " + id + " introuvable");
        }
        return ResponseEntity.ok(product);
    }

    /**
     * Met à jour les informations d’un produit existant.
     *
     * <p>Cette opération est strictement réservée aux administrateurs.
     * Si l’utilisateur n’a pas les droits requis, une erreur HTTP 401 est renvoyée.</p>
     *
     * @param request la requête HTTP contenant le token JWT
     * @param productDto les nouvelles informations à appliquer au produit
     * @return le produit mis à jour ou un message d’erreur s’il est introuvable
     */
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping
    public ResponseEntity<?> setProduct(HttpServletRequest request, @RequestBody ProductDto productDto) {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.unauthorized("Accès non autorisé");
        }

        ProductDto updatedProduct = productService.setProductDetails(productDto);
        if (updatedProduct == null) {
            return ResponseUtils.badRequest("Ce produit n'a pas été trouvé");
        }
        return ResponseEntity.ok(updatedProduct);
    }
}
