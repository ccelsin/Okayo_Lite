package backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import backend.models.Product;

/**
 * Interface de dépôt (repository) pour la gestion des entités {@link Product}.
 * 
 * <p>Cette interface hérite de {@link JpaRepository}, ce qui lui permet de bénéficier
 * automatiquement de toutes les opérations de base (CRUD) sur les produits :
 * création, lecture, mise à jour et suppression.</p>
 *
 * <p>Elle peut également être étendue ultérieurement avec des méthodes
 * personnalisées de recherche selon les conventions Spring Data JPA.</p>
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
}
