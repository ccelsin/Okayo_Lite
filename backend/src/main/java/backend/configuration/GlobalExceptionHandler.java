package backend.configuration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import backend.utilities.ApiError;
import backend.utilities.ResponseUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * Gestion centralisée des exceptions de l’application.
 *
 * <p>Cette classe intercepte les exceptions levées par les contrôleurs
 * et les traduit en réponses HTTP normalisées via {@link ResponseUtils},
 * afin d’assurer un format d’erreur cohérent ({@link ApiError}) côté client.</p>
 *
 * <p>Principaux objectifs :</p>
 * <ul>
 *   <li>Fournir des messages d’erreur clairs et des statuts HTTP appropriés,</li>
 *   <li>Regrouper la logique de gestion d’erreurs en un point unique,</li>
 *   <li>Éviter la duplication de code de traitement d’exceptions dans chaque contrôleur.</li>
 * </ul>
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Gère les erreurs de validation sur les payloads JSON annotés (ex. {@code @Valid}).
     *
     * <p>Retourne un statut 400 et une map « champ → message d’erreur ».</p>
     *
     * @param ex exception de validation Spring (binding des champs invalides)
     * @param request requête HTTP en cause
     * @return réponse 400 avec détails par champ
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, Object> errors = ex.getBindingResult().getFieldErrors()
            .stream()
            .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (existing, replacement) -> existing, LinkedHashMap::new));
        return ResponseUtils.error(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), errors);
    }

    /**
     * Gère les violations de contraintes de validation (ex. {@code @NotNull}, {@code @Email}) hors binding.
     *
     * @param ex exception JSR-380/Bean Validation
     * @param request requête HTTP
     * @return réponse 400 avec la liste des violations « propriété → message »
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, Object> errors = ex.getConstraintViolations().stream()
            .collect(Collectors.toMap(violation -> violation.getPropertyPath().toString(), violation -> violation.getMessage(), (existing, replacement) -> existing, LinkedHashMap::new));
        return ResponseUtils.error(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), errors);
    }

    /**
     * Gère les erreurs de conversion de type des paramètres de requête/path variables.
     *
     * <p>Exemple : un identifiant attendu numérique mais fourni en texte.</p>
     *
     * @param ex exception de mismatch de type
     * @param request requête HTTP
     * @return réponse 400 avec message expliquant le paramètre fautif
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = String.format("Parameter '%s' with value '%s' could not be converted to type '%s'", ex.getName(), ex.getValue(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "" );
        return ResponseUtils.error(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    /**
     * Gère l’absence d’un paramètre requis dans la requête.
     *
     * @param ex exception paramètre manquant
     * @param request requête HTTP
     * @return réponse 400 précisant le nom du paramètre requis
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String message = String.format("Parameter '%s' is required", ex.getParameterName());
        return ResponseUtils.error(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    /**
     * Gère les corps de requêtes illisibles / JSON mal formé.
     *
     * @param ex exception de lecture du message HTTP
     * @param request requête HTTP
     * @return réponse 400 indiquant un JSON mal formé
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.debug("Request body could not be read", ex);
        return ResponseUtils.error(HttpStatus.BAD_REQUEST, "Malformed JSON request", request.getRequestURI());
    }

    /**
     * Gère l’utilisation d’une méthode HTTP non supportée par l’endpoint.
     *
     * @param ex exception méthode non supportée
     * @param request requête HTTP
     * @return réponse 405 avec la méthode fautive
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String message = String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod());
        return ResponseUtils.error(HttpStatus.METHOD_NOT_ALLOWED, message, request.getRequestURI());
    }

    /**
     * Gère les erreurs d’authentification (identifiants invalides, token expiré, etc.).
     *
     * @param ex exception d’authentification Spring Security
     * @param request requête HTTP
     * @return réponse 401 avec le message d’erreur
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        log.debug("Authentication failed: {}", ex.getMessage());
        return ResponseUtils.error(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Gère les erreurs d’autorisation (droits insuffisants).
     *
     * @param ex exception d’accès refusé
     * @param request requête HTTP
     * @return réponse 403 (forbidden)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return ResponseUtils.error(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Gère les cas où une entité demandée n’est pas trouvée en base.
     *
     * @param ex exception JPA « not found »
     * @param request requête HTTP
     * @return réponse 404 avec le message d’erreur
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        return ResponseUtils.error(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Gère les erreurs d’argument illégal transmises à une méthode.
     *
     * @param ex exception d’argument invalide
     * @param request requête HTTP
     * @return réponse 400 avec le détail du problème
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return ResponseUtils.error(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Gère les erreurs d’exécution non anticipées (runtime).
     *
     * <p>Journalise l’erreur au niveau ERROR et renvoie une réponse 500 générique.</p>
     *
     * @param ex exception à l’exécution
     * @param request requête HTTP
     * @return réponse 500 « unexpected error »
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntime(RuntimeException ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        return ResponseUtils.error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request.getRequestURI());
    }

    /**
     * Gère toute autre exception non spécifiquement traitée ci-dessus.
     *
     * <p>Filet de sécurité global renvoyant un statut 500 avec un message générique.</p>
     *
     * @param ex exception non gérée
     * @param request requête HTTP
     * @return réponse 500 avec message d’erreur générique
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        return ResponseUtils.error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request.getRequestURI());
    }
}
