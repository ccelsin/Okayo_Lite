package backend.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import backend.filter.JwtFilter;
import backend.services.user.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;

/**
 * Configuration centrale de la sécurité Spring Security.
 *
 * <p>Responsabilités :</p>
 * <ul>
 *   <li>Déclaration de la chaîne de filtres de sécurité ({@link SecurityFilterChain}),</li>
 *   <li>Configuration de l’authentification avec un {@link AuthenticationManager}
 *       basé sur {@link CustomUserDetailsService} et un {@link PasswordEncoder},</li>
 *   <li>Ouverture des endpoints publics (Swagger/OpenAPI et /api/auth/**),</li>
 *   <li>Ajout du filtre JWT personnalisé {@link JwtFilter} avant le filtre d’authentification standard.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** Service de chargement des utilisateurs pour l’authentification. */
    private final CustomUserDetailsService customUserDetailsService;

    /** Utilitaire JWT pour la validation/signature des tokens (injecté dans JwtFilter). */
    private final JwtUtils jwtUtils;

    /**
     * Déclare le {@link AuthenticationManager} utilisé par Spring Security.
     *
     * <p>Il est construit à partir du {@link CustomUserDetailsService} et
     * du {@link PasswordEncoder} (BCrypt ici) afin de vérifier les identifiants.</p>
     *
     * @param http objet de configuration HTTP partagé
     * @param passwordEncoder l’encodeur de mot de passe à utiliser
     * @return une instance d’{@link AuthenticationManager}
     * @throws Exception en cas d’erreur de construction
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http, PasswordEncoder passwordEncoder) throws Exception{
        AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(customUserDetailsService).passwordEncoder(passwordEncoder);
        return authenticationManagerBuilder.build();
    }

    /**
     * Déclare la chaîne de filtres de sécurité et les règles d’autorisation.
     *
     * <p>Configuration principale :</p>
     * <ul>
     *   <li><strong>CSRF désactivé</strong> (API stateless, usage de JWT),</li>
     *   <li><strong>Endpoints publics</strong> : Swagger/OpenAPI et <code>/api/auth/**</code>,</li>
     *   <li><strong>Tout le reste authentifié</strong>,</li>
     *   <li><strong>Filtre JWT</strong> inséré avant {@link UsernamePasswordAuthenticationFilter}
     *       pour extraire et valider le token sur chaque requête.</li>
     * </ul>
     *
     * @param http le builder de configuration HTTP
     * @return la {@link SecurityFilterChain} prête à l’emploi
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        try {
            return http
                    // API REST stateless : pas de formulaire ni de session côté serveur → CSRF inutile
                    .csrf(AbstractHttpConfigurer::disable)

                    // Règles d'autorisation des requêtes HTTP
                    .authorizeHttpRequests(authorize -> authorize
                            // Documentation Swagger / OpenAPI accessible sans authentification
                            .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**"
                            ).permitAll()
                            // Endpoints d'auth publics (login / register)
                            .requestMatchers("/api/auth/**").permitAll()
                            // Toute autre requête nécessite un token JWT valide
                            .anyRequest().authenticated())

                    // Insertion du filtre JWT avant l'authentification username/password
                    .addFilterBefore(new JwtFilter(customUserDetailsService, jwtUtils), UsernamePasswordAuthenticationFilter.class)
                    .build();
        } catch (Exception e) {
            // Enveloppe l'exception dans une RuntimeException pour simplifier la gestion au démarrage
            throw new RuntimeException(e);
        }
    }

    /**
     * Déclare l’encodeur de mot de passe utilisé par l’application.
     *
     * <p>BCrypt est recommandé pour sa résistance aux attaques par force brute
     * (salage intégré et coût paramétrable).</p>
     *
     * @return un {@link BCryptPasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
