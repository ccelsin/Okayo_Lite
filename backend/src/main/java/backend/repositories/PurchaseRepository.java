package backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import backend.models.Purchase;

/**
 * Interface de dépôt (repository) pour la gestion des entités {@link Purchase}.
 * 
 * <p>Ce dépôt hérite de {@link JpaRepository}, offrant l’ensemble des opérations CRUD
 * ainsi que la possibilité de définir des requêtes personnalisées via les conventions
 * de nommage Spring Data JPA.</p>
 *
 * <p>Les méthodes personnalisées permettent de filtrer les achats selon l’utilisateur
 * acheteur et l’état de confirmation des achats.</p>
 */
@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    /**
     * Recherche tous les achats effectués par un utilisateur spécifique.
     *
     * @param purchaserId l’identifiant de l’utilisateur acheteur
     * @return la liste des achats associés à cet utilisateur
     */
    List<Purchase> findByPurchaserId(Long purchaserId);

    /**
     * Recherche tous les achats qui n’ont pas encore été confirmés.
     *
     * @return la liste des achats non confirmés
     */
    List<Purchase> findByIsConfirmedFalse();

    /**
     * Recherche tous les achats non confirmés effectués par un utilisateur donné.
     *
     * @param purchaserId l’identifiant de l’utilisateur acheteur
     * @return la liste des achats non confirmés de cet utilisateur
     */
    List<Purchase> findByPurchaserIdAndIsConfirmedFalse(Long purchaserId);
}
