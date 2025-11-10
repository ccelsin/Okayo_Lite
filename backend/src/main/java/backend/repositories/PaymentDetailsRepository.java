package backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import backend.models.PaymentDetails;

/**
 * Interface de dépôt (repository) pour la gestion des entités {@link PaymentDetails}.
 * 
 * <p>Cette interface hérite de {@link JpaRepository}, offrant toutes les opérations
 * CRUD standard (création, lecture, mise à jour, suppression) sur les informations
 * de paiement enregistrées dans la base de données.</p>
 *
 * <p>Elle peut être étendue ultérieurement pour inclure des méthodes de recherche
 * personnalisées selon les besoins du projet (ex. filtrage par utilisateur, type de paiement, etc.).</p>
 */
@Repository
public interface PaymentDetailsRepository extends JpaRepository<PaymentDetails, Long> {
    
}
