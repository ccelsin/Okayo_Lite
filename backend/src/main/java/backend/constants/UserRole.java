package backend.constants;

/**
 * Enumération représentant les différents rôles utilisateurs du système.
 *
 * <p>Cette énumération est utilisée pour la gestion des autorisations
 * et des droits d’accès dans l’application (via Spring Security notamment).</p>
 *
 * <ul>
 *   <li>{@link #ADMIN} – Rôle administrateur : possède tous les droits,
 *       peut gérer les produits, factures, utilisateurs, etc.</li>
 *   <li>{@link #CUSTOMER} – Rôle client : limité à la consultation de ses propres
 *       données et à la création de ses achats.</li>
 * </ul>
 */
public enum UserRole {
    /** Utilisateur administrateur du système. */
    ADMIN,

    /** Client ou utilisateur standard. */
    CUSTOMER
}
