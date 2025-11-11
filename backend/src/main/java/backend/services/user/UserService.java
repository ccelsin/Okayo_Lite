package backend.services.user;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import backend.configuration.JwtUtils;
import backend.constants.UserRole;
import backend.dtos.UserDto;
import backend.models.User;
import backend.repositories.UserRepository;
import backend.utilities.BeanCopyUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Service applicatif pour la gestion des utilisateurs (profil, autorisation, rôles, etc.).
 *
 * <p>Ce service interagit avec le dépôt {@link UserRepository} et les utilitaires JWT
 * pour récupérer/mettre à jour les informations des utilisateurs et contrôler l'accès.</p>
 *
 * <p>Annonations:</p>
 * <ul>
 *   <li>{@link Service} : composant Spring injectable.</li>
 *   <li>{@link RequiredArgsConstructor} : injection par constructeur des dépendances final.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserService {

    /** Dépôt d'accès aux entités {@link User}. */
    private final UserRepository userRepository;

    /** Utilitaires JWT (extraction du username, vérification d'expiration, etc.). */
    private final JwtUtils jwtUtils;
    
    /**
     * Récupère le profil (DTO) d'un utilisateur par son identifiant.
     *
     * @param userId identifiant de l'utilisateur
     * @return un {@link UserDto} si trouvé, sinon {@code null}
     */
    public UserDto getProfile(Long userId) {
        return userRepository.findById(userId)
                .map(UserMapperService::toDto)
                .orElse(null);
    }
    
    /**
     * Extrait l'identifiant utilisateur depuis la requête HTTP via l'en-tête Authorization (Bearer).
     *
     * @param request requête HTTP contenant le token JWT
     * @return l'identifiant utilisateur si extraction/résolution OK, sinon {@code null}
     */
    public Long extractUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        return extractUserIdFromToken(token);
    }

    /**
     * Extrait l'identifiant utilisateur depuis un token JWT.
     *
     * @param token JWT
     * @return l'identifiant utilisateur correspondant au username contenu dans le token, ou {@code null} si introuvable
     */
    public Long extractUserIdFromToken(String token) {
        String username = jwtUtils.extractUsername(token);
            User user = userRepository.findByUsername(username);
            if (user != null) {
                return user.getId();
            }
        return null; 
    }

    /**
     * Vérifie si la requête est autorisée (token Bearer valide et non expiré).
     *
     * @param request requête HTTP
     * @return {@code true} si le token existe et n'est pas expiré, sinon {@code false}
     */
    public boolean isAuthorized(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authHeader.substring(7);
        return !jwtUtils.isTokenExpired(token);
    }

    /**
     * Vérifie si l'utilisateur de la requête possède le rôle ADMIN.
     *
     * @param request requête HTTP contenant le token
     * @return {@code true} si l'utilisateur est ADMIN, sinon {@code false}
     */
    public boolean isAdmin(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authHeader.substring(7);
        String username = jwtUtils.extractUsername(token);
        User user = userRepository.findByUsername(username);
        return user != null && user.getRole() == UserRole.ADMIN;
    }

    /**
     * Met à jour le profil d'un utilisateur à partir d'un {@link UserDto}.
     *
     * <p>Copie uniquement les propriétés non nulles depuis le DTO vers l'entité,
     * puis persiste l'utilisateur en base.</p>
     *
     * <p><strong>Note d'implémentation :</strong> en cas de nom d'utilisateur déjà pris,
     * la ligne « new DataIntegrityViolationException(...) » crée une exception mais ne la lance pas.
     * Si tu souhaites bloquer la mise à jour, il faut la <em>lancer</em> (ex: {@code throw new DataIntegrityViolationException(...);})
     * — mais ici je ne modifie pas ton code.</p>
     *
     * @param userId identifiant de l'utilisateur à mettre à jour
     * @param userDto données du profil (champs non nuls pris en compte)
     * @return le profil mis à jour en {@link UserDto}
     * @throws EntityNotFoundException si l'utilisateur est introuvable
     */
    public UserDto setProfile(Long userId, UserDto userDto) {
        User check = userRepository.findByUsername(userDto.username());
        if(check != null){
            new DataIntegrityViolationException("Ce nom d'utilisateur est déjà pris");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("Cet utilisateur n'a pas été retrouvé"));
        
        BeanCopyUtils.copyNonNullProperties(userDto, user);
        user.setId(userId);
        User savedUser = userRepository.save(user);
        return UserMapperService.toDto(savedUser);
    }

    /**
     * Récupère un utilisateur par identifiant et le convertit en DTO.
     *
     * @param id identifiant de l'utilisateur
     * @return {@link UserDto} si trouvé, sinon {@code null}
     */
    public UserDto getUser(Long id) {
        Optional<User> user = userRepository.findById(id);
        return user.map(UserMapperService::toDto).orElse(null);
    }

    /**
     * Récupère tous les utilisateurs et les convertit en DTO.
     *
     * @return liste de {@link UserDto}
     */
    public List<UserDto> getAllUser() {
        List<User> users = userRepository.findAll();
        return UserMapperService.toDtoList(users);
    }

    /**
     * Vérifie si l'utilisateur de la requête possède le rôle CUSTOMER.
     *
     * @param request requête HTTP contenant le token
     * @return {@code true} si l'utilisateur a le rôle CUSTOMER, sinon {@code false}
     */
    public boolean isCustomer(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authHeader.substring(7);
        String username = jwtUtils.extractUsername(token);
        User user = userRepository.findByUsername(username);
        return user != null && user.getRole() == UserRole.CUSTOMER;
    }

    /**
     * Génère un code client aléatoire de la forme {@code CU0000-0000} en s'assurant de son unicité.
     *
     * <p>En cas de collision, relance récursivement la génération jusqu'à obtention d'un code libre.</p>
     *
     * @return code client unique
     */
    public String generateCode() {
        Random random = new Random();

        String part1 = String.format("%04d", random.nextInt(10000)); // 0000 → 9999
        String part2 = String.format("%04d", random.nextInt(10000)); // 0000 → 9999

        String code = "CU" + part1 + "-" + part2;

        Optional <User> customerOpt = userRepository.findByCodeCustomer(code);
        if(customerOpt.isPresent())
        {
                return generateCode();
        }
            return code;
    
    }
    
}
