package backend.repositories;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import backend.models.Tva;

/**
 * Interface de dépôt (repository) pour la gestion des entités {@link Tva}.
 * 
 * <p>Ce dépôt hérite de {@link JpaRepository}, ce qui lui fournit toutes les opérations
 * CRUD standards ainsi que la possibilité de définir des requêtes personnalisées
 * basées sur la nomenclature Spring Data JPA.</p>
 *
 * <p>Les méthodes personnalisées ci-dessous permettent de récupérer les configurations
 * de TVA dont les périodes d’évolution commencent ou se terminent à une date donnée.</p>
 */
@Repository
public interface TvaRepository extends JpaRepository<Tva, Long> {
    
    /**
     * Recherche toutes les configurations de TVA dont :
     * <ul>
     *   <li>la date de début d’évolution n’est pas nulle,</li>
     *   <li>le taux futur est défini,</li>
     *   <li>l’évolution n’a pas encore été appliquée,</li>
     *   <li>et dont la date de début d’évolution est antérieure ou égale à la date spécifiée.</li>
     * </ul>
     *
     * <p>Cette méthode permet d’identifier les taux de TVA qui doivent être activés à la date donnée.</p>
     *
     * @param date la date de référence pour vérifier le début d’évolution
     * @return la liste des taux de TVA à activer
     */
    List<Tva> findAllByStartEvolutionDateNotNullAndFutureRateNotNullAndEvolutionAppliedFalseAndStartEvolutionDateLessThanEqual(Date date);

    /**
     * Recherche toutes les configurations de TVA dont :
     * <ul>
     *   <li>la date de fin d’évolution n’est pas nulle,</li>
     *   <li>l’évolution a déjà été appliquée,</li>
     *   <li>et dont la date de fin d’évolution est antérieure ou égale à la date spécifiée.</li>
     * </ul>
     *
     * <p>Cette méthode permet d’identifier les taux de TVA dont la période d’évolution est terminée,
     * afin de les réinitialiser ou archiver si nécessaire.</p>
     *
     * @param date la date de référence pour vérifier la fin d’évolution
     * @return la liste des taux de TVA dont la période d’évolution est terminée
     */
    List<Tva> findAllByEndEvolutionDateNotNullAndEvolutionAppliedTrueAndEndEvolutionDateLessThanEqual(Date date);
}
