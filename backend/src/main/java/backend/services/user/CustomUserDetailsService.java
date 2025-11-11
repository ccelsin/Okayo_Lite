package backend.services.user;

import backend.models.User;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;

/**
 * Implémentation personnalisée de {@link UserDetailsService} utilisée par Spring Security
 * pour le chargement des informations d’un utilisateur lors de l’authentification.
 *
 * <p>Cette classe convertit une entité {@link User} en un objet {@link UserDetails}
 * reconnu par le framework Spring Security.</p>
 *
 * <p>Annotée avec {@link Service} pour être gérée par le conteneur Spring, et avec
 * {@link RequiredArgsConstructor} pour injecter automatiquement les dépendances via le constructeur.</p>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    /** Dépôt d’accès aux entités {@link User}. */
    private final UserRepository userRepository;

    /**
     * Charge un utilisateur à partir de son nom d’utilisateur.
     *
     * <p>Cette méthode est appelée automatiquement par Spring Security lors du processus
     * d’authentification. Elle récupère l’utilisateur en base et construit un objet
     * {@link org.springframework.security.core.userdetails.User} compatible avec
     * le contexte de sécurité.</p>
     *
     * @param username le nom d’utilisateur à rechercher
     * @return un objet {@link UserDetails} contenant le nom, le mot de passe et les rôles
     * @throws UsernameNotFoundException si aucun utilisateur n’est trouvé avec ce nom
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }
}
