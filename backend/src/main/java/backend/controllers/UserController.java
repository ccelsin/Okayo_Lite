package backend.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import backend.utilities.ResponseUtils;
import backend.dtos.UserDto;
import backend.models.User;
import backend.repositories.UserRepository;
import backend.services.user.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Contrôleur REST gérant les opérations liées aux utilisateurs ({@link User}).
 *
 * <p>Ce contrôleur expose plusieurs endpoints pour :
 * <ul>
 *   <li>Consulter le profil de l’utilisateur connecté,</li>
 *   <li>Mettre à jour son profil,</li>
 *   <li>Consulter un utilisateur spécifique (administrateur),</li>
 *   <li>Lister tous les utilisateurs (administrateur),</li>
 *   <li>Vérifier si l’utilisateur courant est administrateur.</li>
 * </ul>
 *
 * <p>Chaque endpoint est protégé par un jeton JWT validé via {@link UserService}.
 * Les méthodes renvoient des réponses standardisées via {@link ResponseUtils}.</p>
 *
 * <p>Annoté avec :</p>
 * <ul>
 *   <li>{@link RestController} : indique un contrôleur Spring REST,</li>
 *   <li>{@link RequestMapping("/api/user")} : définit la racine de l’API,</li>
 *   <li>{@link RequiredArgsConstructor} : injection automatique des dépendances finales.</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    /** Service métier de gestion des utilisateurs. */
    private final UserService userService;

    /** Dépôt JPA pour accéder directement aux entités {@link User}. */
    private final UserRepository userRepository;

    /**
     * Récupère le profil de l’utilisateur actuellement connecté.
     *
     * <p>Le jeton JWT est extrait de l’en-tête HTTP Authorization,
     * puis validé. Si le token est invalide ou expiré, une réponse
     * HTTP 401 (Unauthorized) est retournée.</p>
     *
     * @param request la requête HTTP contenant le jeton d’authentification
     * @return le profil de l’utilisateur sous forme de {@link UserDto},
     *         ou une erreur d’autorisation si le token est invalide
     */
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (userService.isAuthorized(request) == false) {
            return ResponseUtils.unauthorized("Votre token est invalide");
        }

        String token = authHeader.substring(7);
        Long userId = userService.extractUserIdFromToken(token);
        if (userId == null) {
            return ResponseUtils.unauthorized("Votre token est invalide");
        }

        var userDto = userService.getProfile(userId);
        return ResponseEntity.ok(userDto);
    }

    /**
     * Met à jour les informations du profil utilisateur connecté.
     *
     * <p>La méthode vérifie la validité du jeton, extrait l’identifiant utilisateur
     * et applique les modifications via le service {@link UserService}.
     * Après mise à jour, l’utilisateur doit se reconnecter.</p>
     *
     * @param request la requête HTTP contenant le jeton JWT
     * @param updatedUserDto les nouvelles données du profil
     * @return un message de confirmation ou une erreur d’autorisation
     */
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(HttpServletRequest request, @RequestBody UserDto updatedUserDto) {
        String authHeader = request.getHeader("Authorization");
        if (userService.isAuthorized(request) == false) {
            return ResponseUtils.unauthorized("Votre token est invalide");
        }

        String token = authHeader.substring(7);
        Long userId = userService.extractUserIdFromToken(token);
        if (userId == null) {
            ResponseUtils.unauthorized("Invalid token");
        }
        userService.setProfile(userId, updatedUserDto);
        return ResponseEntity.ok("Profil mis à jour. Vous devez vous reconnecter");
    }

    /**
     * Récupère les informations d’un utilisateur spécifique (réservé aux administrateurs).
     *
     * @param request la requête HTTP contenant le jeton JWT
     * @param id l’identifiant de l’utilisateur recherché
     * @return un {@link UserDto} si trouvé, sinon un message d’erreur adapté
     */
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<?> get(HttpServletRequest request, @PathVariable Long id) {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.forbidden("Vous n'êtes pas autorisé à effectuer cette action");
        }
        
        UserDto customer = userService.getUser(id);
        if (customer == null) {
            return ResponseUtils.notFound("L'utilisateur demandé n'a pas été retrouvé");
        }
        return ResponseEntity.ok(customer);
    }

    /**
     * Récupère la liste complète des utilisateurs (réservé aux administrateurs).
     *
     * <p>Retourne une réponse 403 si l’utilisateur connecté n’est pas administrateur.</p>
     *
     * @param request la requête HTTP contenant le jeton JWT
     * @return une liste d’utilisateurs ou un message d’erreur
     */
    public ResponseEntity<?> getAllCustomer(HttpServletRequest request) {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.forbidden("Vous n'êtes pas autorisé à effectuer cette action");
        }
        List<User> customers = userRepository.findAll();
        return ResponseEntity.ok(customers);
    }

    /**
     * Vérifie si l’utilisateur connecté dispose du rôle administrateur.
     *
     * @param request la requête HTTP contenant le jeton JWT
     * @return {@code true} si l’utilisateur est administrateur, sinon {@code false}
     */
    @GetMapping("/isAdmin")
    public boolean checkIfAdmin(HttpServletRequest request){
        return userService.isAdmin(request);
    }
}
