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

import backend.dtos.PaymentDetailsDto;
import backend.dtos.PaymentDetailsRequest;
import backend.services.paymentdetails.PaymentDetailsService;
import backend.services.user.UserService;
import backend.utilities.ResponseUtils;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Contrôleur REST responsable de la gestion des informations de paiement
 * ({@link backend.models.PaymentDetails}).
 *
 * <p>Ce contrôleur permet :</p>
 * <ul>
 *   <li>de créer des informations de paiement pour un utilisateur,</li>
 *   <li>de consulter la liste des moyens de paiement existants,</li>
 *   <li>d’afficher un moyen de paiement spécifique,</li>
 *   <li>et de mettre à jour les informations de paiement existantes.</li>
 * </ul>
 *
 * <p>Toutes les routes sont sécurisées via JWT, et certaines sont réservées
 * aux administrateurs.</p>
 *
 * <p>Les réponses HTTP sont homogènes et gérées via la classe {@link ResponseUtils}.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment-details")
public class PaymentDetailsController {

    /** Service métier gérant les opérations sur les moyens de paiement. */
    private final PaymentDetailsService paymentDetailsService;

    /** Service permettant la vérification des rôles et des droits utilisateur. */
    private final UserService userService;

    /**
     * Crée de nouvelles informations de paiement pour l’utilisateur connecté.
     *
     * <p>Accessible uniquement aux administrateurs.  
     * Si l’utilisateur connecté n’a pas les droits requis, une réponse HTTP 403 est renvoyée.</p>
     *
     * @param request la requête HTTP contenant le token JWT
     * @param paymentDetailsRequest les données du moyen de paiement à enregistrer
     * @return les informations de paiement enregistrées sous forme de {@link PaymentDetailsDto}
     */
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<?> savePaymentDetails(HttpServletRequest request, @RequestBody PaymentDetailsRequest paymentDetailsRequest) {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.forbidden("Seuls les administrateurs ont le droit de créer des informations de paiement");
        }

        Long userId = userService.extractUserIdFromRequest(request);
        PaymentDetailsDto savedPaymentDetails = paymentDetailsService.savePaymentDetails(userId, paymentDetailsRequest);
        return ResponseEntity.ok(savedPaymentDetails);
    }

    /**
     * Récupère la liste de toutes les informations de paiement enregistrées.
     *
     * <p>Accessible à tout utilisateur authentifié disposant d’un token JWT valide.
     * Si le token est absent ou invalide, une réponse HTTP 401 est renvoyée.</p>
     *
     * @param request la requête HTTP contenant le token JWT
     * @return une liste de {@link PaymentDetailsDto}
     */
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<?> getAllPaymentDetails(HttpServletRequest request) {
        if (userService.isAuthorized(request) == false) {
            return ResponseUtils.unauthorized("Accès non autorisé");
        }
        List<PaymentDetailsDto> paymentDetails = paymentDetailsService.getAllPaymentDetails();
        return ResponseEntity.ok(paymentDetails);
    }

    /**
     * Récupère les détails d’un moyen de paiement spécifique à partir de son identifiant.
     *
     * <p>Si aucun enregistrement correspondant n’est trouvé, une réponse HTTP 401 est renvoyée.</p>
     *
     * @param id l’identifiant du moyen de paiement
     * @return les détails du moyen de paiement sous forme de {@link PaymentDetailsDto}
     * @throws Exception si la requête est invalide ou les données corrompues
     */
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<?> getPaymentDetails(@PathVariable Long id) throws Exception {
        PaymentDetailsDto paymentDetails = paymentDetailsService.getPaymentDetails(id);
        if (paymentDetails == null) {
            return ResponseUtils.unauthorized("Accès non autorisé");
        }
        return ResponseEntity.ok(paymentDetails);
    }

    /**
     * Met à jour les informations de paiement existantes.
     *
     * <p>Accessible uniquement aux administrateurs.
     * Si l’utilisateur n’a pas les droits suffisants, une erreur HTTP 403 est renvoyée.</p>
     *
     * @param request la requête HTTP contenant le token JWT
     * @param paymentDetailsDto les nouvelles données du moyen de paiement
     * @return les informations mises à jour ou une erreur si elles sont introuvables
     */
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping
    public ResponseEntity<?> setPaymentDetails(HttpServletRequest request, @RequestBody PaymentDetailsDto paymentDetailsDto) {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.forbidden("Seuls les administrateurs peuvent modifier des informations de paiement");
        }

        Long userId = userService.extractUserIdFromRequest(request);
        PaymentDetailsDto updatedPaymentDetails = paymentDetailsService.setPaymentDetails(userId, paymentDetailsDto);
        if (updatedPaymentDetails == null) {
            return ResponseUtils.badRequest("Ces informations de paiement n'existent pas");
        }
        return ResponseEntity.ok(updatedPaymentDetails);
    }
}
