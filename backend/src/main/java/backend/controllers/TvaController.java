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

import backend.dtos.TvaDto;
import backend.services.tva.TvaService;
import backend.services.user.UserService;
import backend.utilities.ResponseUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Contrôleur REST responsable de la gestion des taux de TVA ({@link backend.models.Tva}).
 *
 * <p>Ce contrôleur fournit les endpoints nécessaires à la consultation, la création
 * et la mise à jour des taux de TVA dans le système.</p>
 *
 * <p>Toutes les opérations exposées nécessitent une authentification via un token JWT
 * et sont réservées aux utilisateurs disposant du rôle administrateur.</p>
 *
 * <p>Annotations utilisées :</p>
 * <ul>
 *   <li>{@link RestController} : indique un contrôleur REST Spring Boot.</li>
 *   <li>{@link RequestMapping} : définit le préfixe d’URL pour toutes les routes de ce contrôleur (<code>/api/tva</code>).</li>
 *   <li>{@link RequiredArgsConstructor} : génère un constructeur injectant automatiquement les dépendances finales.</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tva")
public class TvaController {

    /** Service métier gérant les opérations sur les taux de TVA. */
    private final TvaService tvaService;

    /** Service utilisateur utilisé pour la vérification des droits d’accès. */
    private final UserService userService;

    /**
     * Récupère la liste complète des taux de TVA enregistrés.
     *
     * <p>Accessible uniquement par les administrateurs authentifiés.
     * En cas d’accès non autorisé, renvoie une réponse HTTP 403.</p>
     *
     * @param request la requête HTTP contenant le token JWT
     * @return une réponse contenant la liste des {@link TvaDto}, ou une erreur d’accès
     */
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<?> getAllTva(HttpServletRequest request) {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.forbidden("Vous n'êtes pas autorisé à effectuer cette action");
        }
        List<TvaDto> tvaList = tvaService.getAllTva();
        return ResponseEntity.ok(tvaList);
    }

    /**
     * Récupère les détails d’un taux de TVA spécifique à partir de son identifiant.
     *
     * @param request la requête HTTP contenant le token JWT
     * @param id l’identifiant du taux de TVA recherché
     * @return les informations du taux de TVA sous forme de {@link TvaDto}
     */
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<?> getTva(HttpServletRequest request, @PathVariable Long id) {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.forbidden("Vous n'êtes pas autorisé à effectuer cette action");
        }
        TvaDto tva = tvaService.getTva(id);
        return ResponseEntity.ok(tva);
    }

    /**
     * Enregistre un nouveau taux de TVA dans la base de données.
     *
     * <p>La requête doit contenir un objet {@link TvaDto} valide.
     * En cas de valeurs incorrectes, une exception est levée par le service métier.</p>
     *
     * @param request la requête HTTP contenant le token JWT
     * @param tva les informations du taux de TVA à créer
     * @return le taux de TVA enregistré sous forme de {@link TvaDto}
     * @throws Exception si les données envoyées ne respectent pas les contraintes de validation
     */
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<?> saveTva(HttpServletRequest request, @RequestBody TvaDto tva) throws Exception {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.forbidden("Vous n'êtes pas autorisé à effectuer cette action");
        }
        TvaDto savedTva = tvaService.saveTva(tva);
        return ResponseEntity.ok(savedTva);
    }

    /**
     * Met à jour les informations d’un taux de TVA existant.
     *
     * <p>Cette opération applique également la logique d’évolution planifiée
     * (ex. : changement automatique des taux selon des dates).</p>
     *
     * @param request la requête HTTP contenant le token JWT
     * @param tvaDetails les nouvelles valeurs à appliquer au taux de TVA
     * @return le taux de TVA mis à jour sous forme de {@link TvaDto}
     */
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping
    public ResponseEntity<?> setTva(HttpServletRequest request, @RequestBody TvaDto tvaDetails) {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.forbidden("Vous n'êtes pas autorisé à effectuer cette action");
        }
        TvaDto updatedTva = tvaService.setTva(tvaDetails);
        return ResponseEntity.ok(updatedTva);
    }
}
