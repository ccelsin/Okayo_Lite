package backend.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import backend.configuration.JwtUtils;
import backend.constants.UserRole;
import backend.dtos.AuthUserDto;
import backend.dtos.RegisterDto;
import backend.models.User;
import backend.repositories.UserRepository;
import backend.services.user.UserService;
import backend.utilities.ResponseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Contrôleur REST d’authentification.
 *
 * <p>Expose deux endpoints publics :</p>
 * <ul>
 *   <li><strong>/register</strong> : inscription d’un nouvel utilisateur,</li>
 *   <li><strong>/login</strong> : authentification et émission d’un jeton JWT.</li>
 * </ul>
 *
 * <p>Utilise Spring Security pour l’authentification et un utilitaire {@link JwtUtils}
 * pour la génération de tokens. Les réponses d’erreur sont standardisées via {@link ResponseUtils}.</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    /** Dépôt JPA pour les entités {@link User}. */
    private final UserRepository userRepository;

    /** Encodeur de mots de passe (BCrypt ou équivalent). */
    private final PasswordEncoder passwordEncoder;

    /** Utilitaire JWT (génération/validation de jetons). */
    private final JwtUtils jwtUtils;

    /** Gestionnaire d’authentification Spring Security. */
    private final AuthenticationManager authenticationManager;

    /** Service utilisateur (ex. génération de code client). */
    private final UserService userService;
    
    /**
     * Inscrit un nouvel utilisateur.
     *
     * <p>Étapes :</p>
     * <ol>
     *   <li>Vérifie l’unicité du nom d’utilisateur.</li>
     *   <li>Encode le mot de passe.</li>
     *   <li>Assigne un rôle (par défaut {@link UserRole#ADMIN} si non fourni).</li>
     *   <li>Si le rôle est {@link UserRole#CUSTOMER}, génère un code client unique.</li>
     *   <li>Persiste l’utilisateur puis renvoie le payload d’inscription.</li>
     * </ol>
     *
     * @param authUserDto données d’inscription (username, password, rôle)
     * @return 200 OK avec le DTO d’entrée en cas de succès, 409 si le username est déjà pris,
     *         500 en cas d’erreur serveur
     */
    @PostMapping("/register")
    public ResponseEntity <?> register(@RequestBody RegisterDto authUserDto) {
        try {
            if (userRepository.findByUsername(authUserDto.username()) != null) {
                return ResponseUtils.conflict("Ce nom d'utilisateur est déjà pris. Veuillez en choisir un autre");
            }
            User user = new User();
            user.setUsername(authUserDto.username());
            user.setPassword(passwordEncoder.encode(authUserDto.password()));
            UserRole role = authUserDto.role() != null ? authUserDto.role() : UserRole.ADMIN;
            user.setRole(role);
            if (role == UserRole.CUSTOMER) {
                String code = userService.generateCode();
                user.setCodeCustomer(code);
            }
            userRepository.save(user);
            return ResponseEntity.ok(authUserDto);
        } catch (Exception e) {
            log.error("Failed to register user", e);
            return ResponseUtils.internalServerError("Failed to register user");
        }
        
    }

    /**
     * Authentifie un utilisateur et émet un jeton JWT.
     *
     * <p>Le couple (username, password) est validé via {@link AuthenticationManager}.
     * En cas de succès, renvoie un corps JSON contenant :</p>
     * <ul>
     *   <li><code>token</code> : le JWT signé,</li>
     *   <li><code>type</code> : le préfixe d’authentification (ex. <em>Bearer</em>).</li>
     * </ul>
     *
     * @param authUserDto identifiants d’authentification
     * @return 200 OK avec le token, ou 401 si l’authentification échoue
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthUserDto authUserDto) {

        try{
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authUserDto.username(), authUserDto.password()));
            if (authentication.isAuthenticated()) {
                Map<String, Object> authData = new HashMap<>();
                authData.put("token", jwtUtils.generateToken(authUserDto.username()));
                authData.put("type", "Bearer");
                return ResponseEntity.ok(authData);
            } 
                return ResponseUtils.unauthorized("Nom d'utilisateur ou mot de passe invalide");

        } catch (AuthenticationException e) {
            return ResponseUtils.unauthorized("Nom d'utilisateur ou mot de passe invalide");
        }
    }
    
}
