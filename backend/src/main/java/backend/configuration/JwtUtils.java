package backend.configuration;

import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Classe utilitaire permettant la gestion des JSON Web Tokens (JWT).
 *
 * <p>Elle gère la création, la signature, l’extraction et la validation
 * des tokens utilisés pour l’authentification dans l’application.</p>
 *
 * <p>Les propriétés suivantes sont injectées depuis le fichier de configuration
 * <code>application.properties</code> :</p>
 * <ul>
 *   <li><b>jwt.secret-key</b> – clé secrète utilisée pour signer les tokens.</li>
 *   <li><b>jwt.expiration-time</b> – durée de validité du token (en millisecondes).</li>
 * </ul>
 *
 * <p>Les tokens sont signés à l’aide de l’algorithme HMAC-SHA256
 * ({@link SignatureAlgorithm#HS256}).</p>
 */
@Component
public class JwtUtils {
    
    /** Clé secrète utilisée pour signer et vérifier les tokens JWT. */
    @Value("${jwt.secret-key}")
    private String secretKey;

    /** Durée de validité du token en millisecondes. */
    @Value("${jwt.expiration-time}")
    private long expirationTime;

    // -------------------------------------------------------------------------
    // GÉNÉRATION DE TOKEN
    // -------------------------------------------------------------------------

    /**
     * Génère un token JWT pour un utilisateur donné.
     *
     * @param username le nom d’utilisateur pour lequel le token est créé
     * @return le token JWT signé sous forme de chaîne de caractères
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }

    /**
     * Crée un token JWT signé avec les données spécifiées.
     *
     * @param claims  les données personnalisées à inclure dans le token (payload)
     * @param subject le sujet du token (généralement le nom d’utilisateur)
     * @return le token JWT complet et signé
     */
    private String createToken(Map<String, Object> claims, String subject){
        return Jwts.builder()
                .setClaims(claims)                                  // Données embarquées
                .setSubject(subject)                                 // Identifiant principal du token
                .setIssuedAt(new Date(System.currentTimeMillis()))   // Date de création
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime)) // Date d’expiration
                .signWith(getSignKey(), SignatureAlgorithm.HS256)    // Signature avec clé secrète
                .compact();
    }

    // -------------------------------------------------------------------------
    // SIGNATURE ET CLÉ
    // -------------------------------------------------------------------------

    /**
     * Construit la clé de signature HMAC à partir de la clé secrète configurée.
     *
     * @return une instance {@link Key} utilisable pour la signature JWT
     */
    private Key getSignKey(){
        byte[] keyBytes = secretKey.getBytes();
        return new SecretKeySpec(keyBytes, SignatureAlgorithm.HS256.getJcaName());
    }

    // -------------------------------------------------------------------------
    // EXTRACTION DE CLAIMS
    // -------------------------------------------------------------------------

    /**
     * Extrait tous les <em>claims</em> (informations du payload)
     * contenus dans un token JWT.
     *
     * @param token le token JWT à analyser
     * @return un objet {@link Claims} contenant les données du token
     */
    private Claims extractAllClaims(String token){
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey()) // Vérifie la signature avec la clé secrète
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extrait une donnée spécifique (claim) du token via une fonction de résolution.
     *
     * @param <T> le type du résultat attendu
     * @param token le token JWT
     * @param claimsResolver fonction appliquée aux claims pour en extraire une valeur
     * @return la valeur extraite du token
     */
    private <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver){
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // -------------------------------------------------------------------------
    // EXTRACTION D'INFORMATIONS
    // -------------------------------------------------------------------------

    /**
     * Extrait le nom d’utilisateur (subject) contenu dans le token.
     *
     * @param token le token JWT
     * @return le nom d’utilisateur associé
     */
    public String extractUsername(String token){
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Vérifie si le token JWT est expiré.
     *
     * @param token le token JWT
     * @return {@code true} si le token est expiré, sinon {@code false}
     */
    public boolean isTokenExpired(String token){
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    // -------------------------------------------------------------------------
    // VALIDATION DU TOKEN
    // -------------------------------------------------------------------------

    /**
     * Valide un token JWT en comparant le nom d’utilisateur et la date d’expiration.
     *
     * @param token le token JWT à valider
     * @param userDetails les détails de l’utilisateur courant (issus de Spring Security)
     * @return {@code true} si le token est valide, sinon {@code false}
     */
    public boolean validateToken(String token, UserDetails userDetails){
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
