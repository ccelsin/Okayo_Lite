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

import backend.dtos.PurchaseDto;
import backend.services.purchase.PurchaseService;
import backend.services.user.UserService;
import backend.utilities.ResponseUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Contrôleur REST gérant les opérations liées aux achats ({@link backend.models.Purchase}).
 *
 * <p>Ce contrôleur permet :</p>
 * <ul>
 *   <li>la création d’un nouvel achat,</li>
 *   <li>la consultation d’achats (tous ou spécifiques),</li>
 *   <li>la mise à jour d’un achat existant,</li>
 *   <li>la récupération des achats d’un client particulier ou de ceux en attente de validation.</li>
 * </ul>
 *
 * <p>L’accès à ces endpoints est sécurisé : certaines actions sont réservées
 * aux clients (pour créer un achat) et d’autres aux administrateurs.</p>
 *
 * <p>Les réponses sont uniformisées grâce à la classe utilitaire {@link ResponseUtils}.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/purchase")
public class PurchaseController {

    /** Service métier gérant la logique liée aux achats. */
    private final PurchaseService purchaseService;

    /** Service gérant les utilisateurs et les vérifications de rôle/autorisation. */
    private final UserService userService;

    /**
     * Crée un nouvel achat pour l’utilisateur connecté (client uniquement).
     *
     * <p>Le token JWT est extrait depuis la requête HTTP pour identifier l’acheteur.
     * Si l’utilisateur n’est pas un client, une erreur 403 (forbidden) est retournée.</p>
     *
     * @param request la requête HTTP contenant le jeton JWT
     * @param purchaseDto les données de l’achat à enregistrer
     * @return l’achat enregistré sous forme de {@link PurchaseDto}, ou une erreur d’accès
     */
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<?> savePurchase(HttpServletRequest request, @RequestBody PurchaseDto purchaseDto) {
        if (userService.isCustomer(request) == false) {
            return ResponseUtils.forbidden("Les clients sont les seuls à pouvoir faire des achats");
        }

        Long purchaserId = userService.extractUserIdFromRequest(request);
        if (purchaserId == null) {
            return ResponseUtils.unauthorized("Vous n'êtes pas autorisé à faire d'achat.");
        }

        PurchaseDto sanitizedPurchase = new PurchaseDto(
            null,
            purchaseDto.productId(),
            purchaseDto.name(),
            purchaseDto.quantity(),
            purchaseDto.unitPriceHT(),
            purchaseDto.totalHT(),
            purchaseDto.tvaApplied(),
            purchaseDto.totalTva(),
            null,
            purchaserId,
            Boolean.FALSE
        );
        PurchaseDto savedPurchase = purchaseService.savePurchase(sanitizedPurchase);
        return ResponseEntity.ok(savedPurchase);
    }

    /**
     * Récupère la liste complète des achats (administrateur uniquement).
     *
     * @param request la requête HTTP contenant le jeton JWT
     * @return une liste de {@link PurchaseDto}, ou une erreur 401 si non autorisé
     */
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<?> getAllPurchases(HttpServletRequest request) {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.unauthorized("Vous n'êtes pas autorisé à faire cette action");
        }
        List<PurchaseDto> purchases = purchaseService.getAllPurchases();
        return ResponseEntity.ok(purchases);
    }

    /**
     * Récupère les détails d’un achat spécifique via son identifiant.
     *
     * @param id l’identifiant de l’achat recherché
     * @return les détails de l’achat ou une erreur 404 s’il n’existe pas
     */
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<?> getPurchase(@PathVariable Long id) {
        PurchaseDto purchase = purchaseService.getPurchase(id);
        if (purchase == null) {
            return ResponseUtils.notFound("Cet achat n'a pas été retrouvé");
        }
        return ResponseEntity.ok(purchase);
    }

    /**
     * Récupère tous les achats effectués par l’utilisateur actuellement connecté.
     *
     * @param request la requête HTTP contenant le jeton JWT
     * @return la liste des achats du client connecté
     */
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/mine")
    public ResponseEntity<?> getPurchaseByPurchaser(HttpServletRequest request) {
        Long purchaserId = userService.extractUserIdFromRequest(request);
        if (purchaserId == null) {
            return ResponseUtils.unauthorized("Accès non autorisé");
        }

        List<PurchaseDto> purchases = purchaseService.getPurchasesByPurchaser(purchaserId);
        return ResponseEntity.ok(purchases);
    }

    /**
     * Récupère les achats d’un client spécifique (administrateur uniquement).
     *
     * @param id l’identifiant du client
     * @param request la requête HTTP contenant le jeton JWT
     * @return la liste des achats du client
     */
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}/customer")
    public ResponseEntity<?> getPurchaseOfPurchaser(@PathVariable Long id, HttpServletRequest request) {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.unauthorized("Accès non autorisé");
        }

        List<PurchaseDto> purchases = purchaseService.getPurchasesByPurchaser(id);
        return ResponseEntity.ok(purchases);
    }

    /**
     * Récupère les achats en attente de confirmation pour un client donné.
     *
     * @param id l’identifiant du client
     * @param request la requête HTTP contenant le jeton JWT
     * @return la liste des achats en attente
     */
    @GetMapping("/{id}/customer/pending")
    public ResponseEntity<?> getPurchaseOfPurchaserPending(@PathVariable Long id, HttpServletRequest request) {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.unauthorized("Accès non autorisé");
        }

        List<PurchaseDto> purchases = purchaseService.getPurchasesByPurchaserPending(id);
        return ResponseEntity.ok(purchases);
    }

    /**
     * Met à jour un achat existant (ex. : confirmation d’un achat rattaché à une facture).
     *
     * <p>Seuls les administrateurs peuvent modifier les achats.
     * En cas d’état ou d’identifiant invalide, une réponse d’erreur est renvoyée.</p>
     *
     * @param request la requête HTTP contenant le jeton JWT
     * @param purchaseDto les nouvelles informations de l’achat
     * @return l’achat mis à jour ou un message d’erreur approprié
     */
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping
    public ResponseEntity<?> setPurchase(HttpServletRequest request, @RequestBody PurchaseDto purchaseDto) {
        if (userService.isAdmin(request) == false) {
            ResponseUtils.unauthorized("Accès non autorisé");
        }
        
        PurchaseDto purchaseDtoUpdated = new PurchaseDto(
            purchaseDto.id(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            purchaseDto.invoiceId(),
            null,
            purchaseDto.isConfirmed()
        );

        try {
            PurchaseDto updatedPurchase = purchaseService.setPurchase(purchaseDtoUpdated);
            return ResponseEntity.ok(updatedPurchase);
        } catch (EntityNotFoundException ex) {
            return ResponseUtils.notFound(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseUtils.conflict(ex.getMessage());
        }
    }

    /**
     * Récupère la liste de tous les achats en attente de confirmation (administrateur uniquement).
     *
     * @param request la requête HTTP contenant le jeton JWT
     * @return la liste des achats non confirmés
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPurchasePending(HttpServletRequest request) {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.unauthorized("Accès non autorisé");
        }
        Long purchaserId = userService.extractUserIdFromRequest(request);
        if (purchaserId == null) {
            return ResponseUtils.unauthorized("Accès non autorisé");
        }

        List<PurchaseDto> purchases = purchaseService.getPurchasePending();
        return ResponseEntity.ok(purchases);
    }
}
