package backend.utilities;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Classe utilitaire permettant de générer facilement des réponses HTTP cohérentes
 * pour les erreurs de l’API.
 *
 * <p>Elle centralise la création d’instances de {@link ResponseEntity<ApiError>}
 * afin d’assurer un format standardisé des messages d’erreur dans toute l’application.</p>
 *
 * <p>Chaque méthode correspond à un code HTTP particulier (400, 401, 403, etc.) et
 * renvoie un objet {@link ApiError} contenant les détails de l’erreur.</p>
 *
 * <p>Exemple d’utilisation :
 * <pre>{@code
 * return ResponseUtils.badRequest("Paramètre invalide");
 * }</pre>
 * Cela retournera une réponse HTTP 400 avec un corps JSON cohérent.</p>
 */
public final class ResponseUtils {

    /**
     * Constructeur privé pour empêcher l’instanciation de cette classe utilitaire.
     */
    private ResponseUtils() {
    }

    // -------------------------------------------------------------------------
    // Méthodes simplifiées par type d'erreur
    // -------------------------------------------------------------------------

    /**
     * Retourne une réponse HTTP 400 (Bad Request).
     *
     * @param message message d’erreur à afficher
     * @return une {@link ResponseEntity} contenant un objet {@link ApiError}
     */
    public static ResponseEntity<ApiError> badRequest(String message) {
        return error(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Retourne une réponse HTTP 401 (Unauthorized).
     *
     * <p>Utilisée lorsqu’un utilisateur non authentifié tente d’accéder
     * à une ressource protégée.</p>
     *
     * @param message message d’erreur à afficher
     * @return une réponse HTTP 401 avec un {@link ApiError}
     */
    public static ResponseEntity<ApiError> unauthorized(String message) {
        return error(HttpStatus.UNAUTHORIZED, message);
    }

    /**
     * Retourne une réponse HTTP 403 (Forbidden).
     *
     * <p>Utilisée lorsqu’un utilisateur authentifié tente d’accéder
     * à une ressource sans disposer des droits nécessaires.</p>
     *
     * @param message message d’erreur
     * @return une réponse HTTP 403 avec un {@link ApiError}
     */
    public static ResponseEntity<ApiError> forbidden(String message) {
        return error(HttpStatus.FORBIDDEN, message);
    }

    /**
     * Retourne une réponse HTTP 404 (Not Found).
     *
     * <p>Utilisée lorsqu’une ressource demandée n’existe pas
     * (ex. entité en base de données non trouvée).</p>
     *
     * @param message message d’erreur
     * @return une réponse HTTP 404 avec un {@link ApiError}
     */
    public static ResponseEntity<ApiError> notFound(String message) {
        return error(HttpStatus.NOT_FOUND, message);
    }

    /**
     * Retourne une réponse HTTP 409 (Conflict).
     *
     * <p>Utilisée en cas de conflit logique ou de violation d’unicité
     * (ex. nom d’utilisateur déjà utilisé).</p>
     *
     * @param message message d’erreur
     * @return une réponse HTTP 409 avec un {@link ApiError}
     */
    public static ResponseEntity<ApiError> conflict(String message) {
        return error(HttpStatus.CONFLICT, message);
    }

    /**
     * Retourne une réponse HTTP 500 (Internal Server Error).
     *
     * <p>Utilisée pour les erreurs inattendues côté serveur.</p>
     *
     * @param message message d’erreur
     * @return une réponse HTTP 500 avec un {@link ApiError}
     */
    public static ResponseEntity<ApiError> internalServerError(String message) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    // -------------------------------------------------------------------------
    // Méthodes génériques de création d’erreur
    // -------------------------------------------------------------------------

    /**
     * Crée une réponse d’erreur HTTP avec un statut et un message.
     *
     * @param status code de statut HTTP
     * @param message message d’erreur
     * @return réponse HTTP contenant un {@link ApiError}
     */
    public static ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return error(status, message, null, null);
    }

    /**
     * Crée une réponse d’erreur HTTP avec un statut, message et chemin de requête.
     *
     * @param status code de statut HTTP
     * @param message message d’erreur
     * @param path URI ou endpoint où l’erreur est survenue
     * @return réponse HTTP avec un {@link ApiError}
     */
    public static ResponseEntity<ApiError> error(HttpStatus status, String message, String path) {
        return error(status, message, path, null);
    }

    /**
     * Crée une réponse d’erreur HTTP complète avec statut, message, chemin et détails.
     *
     * <p>Permet d’ajouter des informations supplémentaires (ex. erreurs de validation).</p>
     *
     * @param status code HTTP
     * @param message message d’erreur
     * @param path URI ou endpoint concerné
     * @param details map d’informations complémentaires
     * @return une {@link ResponseEntity} contenant un {@link ApiError}
     */
    public static ResponseEntity<ApiError> error(HttpStatus status, String message, String path, Map<String, Object> details) {
        return ResponseEntity.status(status).body(new ApiError(status, message, path, details));
    }
}
