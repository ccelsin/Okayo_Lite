package backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import backend.models.Invoice;

/**
 * Interface de dépôt (repository) pour la gestion des entités {@link Invoice}.
 * 
 * <p>Ce dépôt hérite de {@link JpaRepository}, offrant toutes les opérations
 * de base (CRUD) sur les factures ainsi que la possibilité de définir
 * des requêtes personnalisées selon les conventions de Spring Data JPA.</p>
 *
 * <p>Les méthodes personnalisées permettent ici de rechercher une facture
 * par sa référence unique ou de récupérer toutes les factures confirmées
 * d’un client spécifique.</p>
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    
    /**
     * Recherche une facture à partir de sa référence unique.
     *
     * @param reference la référence de la facture à rechercher
     * @return un {@link Optional} contenant la facture si elle existe, ou vide sinon
     */
    Optional<Invoice> findByReference(String reference);

    /**
     * Recherche toutes les factures confirmées associées à un client donné.
     *
     * @param id l’identifiant du client
     * @return la liste des factures confirmées appartenant à ce client
     */
    List<Invoice> findByCustomerIdAndIsConfirmedTrue(Long id);
}
