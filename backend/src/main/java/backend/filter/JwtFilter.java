package backend.filter;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import backend.configuration.JwtUtils;
import backend.services.user.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Filtre JWT exécuté une seule fois par requête.
 *
 * <p>Responsabilités :</p>
 * <ul>
 *   <li>Intercepter chaque requête entrante,</li>
 *   <li>Extraire le token JWT de l’en-tête {@code Authorization},</li>
 *   <li>Valider le token via {@link JwtUtils},</li>
 *   <li>Charger l’utilisateur et remplir le {@link SecurityContextHolder} si le token est valide.</li>
 * </ul>
 *
 * <p>Le filtre contourne explicitement les endpoints Swagger/OpenAPI pour éviter
 * de bloquer la documentation.</p>
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    /** Service de récupération des utilisateurs (Spring Security). */
    private final CustomUserDetailsService customUserDetailsService;

    /** Utilitaire JWT pour l’extraction/validation des tokens. */
    private final JwtUtils jwtUtils;

    /**
     * Point d’entrée du filtre pour chaque requête HTTP.
     *
     * <p>Étapes :</p>
     * <ol>
     *   <li>Ignore les requêtes vers Swagger/OpenAPI,</li>
     *   <li>Récupère l’en-tête {@code Authorization} et extrait le token (préfixe "Bearer "),</li>
     *   <li>Extrait le nom d’utilisateur du token,</li>
     *   <li>Si le contexte de sécurité est vide, charge les {@link UserDetails} et valide le token,</li>
     *   <li>Alimente le contexte avec un {@link UsernamePasswordAuthenticationToken} si tout est OK,</li>
     *   <li>Laisse passer les requêtes {@code OPTIONS} (CORS preflight),</li>
     *   <li>Passe la main au filtre suivant ; en cas d’erreur, renvoie 401.</li>
     * </ol>
     *
     * @param request  requête HTTP entrante
     * @param response réponse HTTP sortante
     * @param filterChain chaîne de filtres continue
     * @throws ServletException en cas d’erreur de filtre
     * @throws IOException en cas d’erreur d’E/S
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Skip Swagger / OpenAPI
        String path = request.getRequestURI();
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui") || path.equals("/swagger-ui.html")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");        
        String jwt = null;
        String username = null;

        // Récupère le JWT depuis l'en-tête Authorization si présent
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            // Extrait le username contenu dans le token
            username = jwtUtils.extractUsername(jwt);
        }

        // Si un username a été extrait et que le contexte n'est pas déjà authentifié
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null){
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

            // Valide le token; si valide, initialise l'authentification Spring Security
            if (jwtUtils.validateToken(jwt, userDetails)){
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Laisse passer les requêtes preflight CORS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Poursuit la chaîne; en cas d'erreur, renvoie 401
        try{
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized: " + e.getMessage());
        }
    }
}
