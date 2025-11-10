package backend.utilities;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

/**
 * Représente le modèle standard d’erreur retourné par l’API.
 *
 * <p>Cette classe définit la structure JSON commune des réponses d’erreur
 * envoyées au client, notamment lors des exceptions interceptées par
 * {@link backend.configuration.GlobalExceptionHandler}.</p>
 *
 * <p>Elle permet de garantir une uniformité des messages d’erreur
 * dans toute l’application.</p>
 *
 * <p>Exemple de réponse JSON :</p>
 * <pre>{@code
 * {
 *   "timestamp": "2025-11-10T12:34:56.789Z",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "La ressource demandée est introuvable",
 *   "path": "/api/users/42"
 * }
 * }</pre>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL) // Ignore les champs nuls dans la réponse JSON
public class ApiError {

    /** Date et heure de l’occurrence de l’erreur (UTC). */
    private final Instant timestamp = Instant.now();

    /** Code de statut HTTP associé à l’erreur (ex : 404, 500, etc.). */
    private final int status;

    /** Libellé court du statut HTTP (ex : "Not Found", "Bad Request", etc.). */
    private final String error;

    /** Message d’erreur détaillé destiné au client. */
    private final String message;

    /** Chemin ou endpoint où l’erreur est survenue. */
    private final String path;

    /**
     * Détails supplémentaires de l’erreur (ex : erreurs de validation).
     *
     * <p>Cette map contient des informations complémentaires, souvent sous la forme :
     * <pre>{@code
     * {
     *   "field": "Le champ 'email' est obligatoire"
     * }
     * }</pre></p>
     */
    private final Map<String, Object> details;

    // -------------------------------------------------------------------------
    // Constructeurs simplifiés
    // -------------------------------------------------------------------------

    /**
     * Constructeur minimal avec statut et message.
     *
     * @param status code de statut HTTP
     * @param message message d’erreur
     */
    public ApiError(HttpStatus status, String message) {
        this(status, message, null, null);
    }

    /**
     * Constructeur avec statut, message et chemin de la requête.
     *
     * @param status code HTTP
     * @param message message d’erreur
     * @param path chemin ou URI de la requête ayant causé l’erreur
     */
    public ApiError(HttpStatus status, String message, String path) {
        this(status, message, path, null);
    }

    /**
     * Constructeur complet avec tous les champs.
     *
     * @param status code HTTP
     * @param message message d’erreur
     * @param path URI concerné
     * @param details informations supplémentaires (facultatives)
     */
    public ApiError(HttpStatus status, String message, String path, Map<String, Object> details) {
        this.status = status.value();             // ex. 404
        this.error = status.getReasonPhrase();    // ex. "Not Found"
        this.message = message;                   // message personnalisé
        this.path = path;                         // endpoint concerné
        this.details = details;                   // détails optionnels (peut être null)
    }
}
