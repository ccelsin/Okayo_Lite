package backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import backend.models.User;

/**
 * Interface de dépôt (repository) pour la gestion des entités {@link User}.
 * 
 * <p>Cette interface hérite de {@link JpaRepository}, ce qui lui permet de bénéficier
 * de toutes les opérations CRUD (création, lecture, mise à jour, suppression)
 * ainsi que de fonctionnalités avancées telles que la pagination et les requêtes personnalisées.</p>
 *
 * <p>Les méthodes supplémentaires définies ici permettent de rechercher des utilisateurs
 * selon leur nom d'utilisateur ou leur code client spécifique.</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Recherche un utilisateur à partir de son nom d'utilisateur.
     *
     * @param username le nom d'utilisateur à rechercher
     * @return l'utilisateur correspondant, ou {@code null} s'il n'existe pas
     */
    User findByUsername(String username);

    /**
     * Recherche un utilisateur à partir de son code client.
     *
     * @param code le code client à rechercher
     * @return un {@link Optional} contenant l'utilisateur s'il existe, ou vide sinon
     */
    Optional<User> findByCodeCustomer(String code);
}
