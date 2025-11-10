package backend.services.tva;

import java.util.Date;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import backend.models.Tva;
import backend.repositories.TvaRepository;
import lombok.RequiredArgsConstructor;

/**
 * Service planificateur responsable de l’application automatique
 * des évolutions de taux de TVA programmées.
 *
 * <p>Ce service est exécuté périodiquement grâce à l’annotation {@link Scheduled},
 * et applique ou annule les évolutions de taux selon les dates définies
 * dans chaque entité {@link Tva}.</p>
 *
 * <p>Annoté avec {@link Service} pour être géré par le conteneur Spring et
 * {@link RequiredArgsConstructor} pour l’injection automatique du dépôt.</p>
 *
 * <p>L’annotation {@link Transactional} garantit la cohérence des données lors
 * de la mise à jour en masse des taux.</p>
 */
@Service
@RequiredArgsConstructor
public class TvaEvolutionScheduler {

    /** Dépôt d’accès aux entités {@link Tva}. */
    private final TvaRepository tvaRepository;

    /**
     * Tâche planifiée exécutée automatiquement toutes les heures (3600000 ms).
     *
     * <p>Cette méthode vérifie deux cas :</p>
     * <ul>
     *   <li><strong>Application :</strong> active les taux futurs dont la date de début est atteinte.</li>
     *   <li><strong>Réversion :</strong> rétablit les anciens taux lorsque la date de fin d’évolution est dépassée.</li>
     * </ul>
     *
     * <p>Les entités mises à jour sont sauvegardées en base via le {@link TvaRepository}.</p>
     */
    @Scheduled(fixedRate = 3600000) // Toutes les 1 heure
    @Transactional
    public void applyPlannedEvolutions() {
        Date today = new Date(System.currentTimeMillis());

        // -------------------------------------------------------------
        // Application des évolutions dont la date de début est atteinte
        // -------------------------------------------------------------
        List<Tva> toApply = tvaRepository
            .findAllByStartEvolutionDateNotNullAndFutureRateNotNullAndEvolutionAppliedFalseAndStartEvolutionDateLessThanEqual(today);

        for (Tva tva : toApply) {
            // Sauvegarde du taux actuel comme précédent
            tva.setPreviousRate(tva.getDefaultRate());
            // Application du nouveau taux
            tva.setDefaultRate(tva.getFutureRate());
            // Marquage de l’évolution comme appliquée
            tva.setEvolutionApplied(true);
            // Persistance des modifications
            tvaRepository.save(tva);
        }

        // -------------------------------------------------------------
        // Réversion des évolutions dont la date de fin est atteinte
        // -------------------------------------------------------------
        List<Tva> toRevert = tvaRepository
            .findAllByEndEvolutionDateNotNullAndEvolutionAppliedTrueAndEndEvolutionDateLessThanEqual(today);

        for (Tva tva : toRevert) {
            // Rétablissement du taux précédent
            if (tva.getPreviousRate() != null) {
                tva.setDefaultRate(tva.getPreviousRate());
            }
            // Réinitialisation des champs liés à l’évolution
            tva.setPreviousRate(null);
            tva.setFutureRate(null);
            tva.setStartEvolutionDate(null);
            tva.setEndEvolutionDate(null);
            tva.setEvolutionApplied(false);
            tvaRepository.save(tva);
        }
    }
}
