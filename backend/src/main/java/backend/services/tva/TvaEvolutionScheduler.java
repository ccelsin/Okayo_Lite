package backend.services.tva;

import java.util.Date;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import backend.models.Tva;
import backend.repositories.TvaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TvaEvolutionScheduler {

    private final TvaRepository tvaRepository;

    // Every 1 hours
    @Scheduled(fixedRate = 3600000)

    @Transactional
    public void applyPlannedEvolutions() {
        Date today = new Date(System.currentTimeMillis());


        // Apply evolutions whose start date is reached
        List<Tva> toApply = tvaRepository
            .findAllByStartEvolutionDateNotNullAndFutureRateNotNullAndEvolutionAppliedFalseAndStartEvolutionDateLessThanEqual(today);

        for (Tva tva : toApply) {
            //Save default rate as previous
            tva.setPreviousRate(tva.getDefaultRate());
            // Apply new rate
            tva.setDefaultRate(tva.getFutureRate());
            // Mark as applied
            tva.setEvolutionApplied(true);
            // Save changes
            tvaRepository.save(tva);
        }

        // Revert evolutions whose end date is reached
        List<Tva> toRevert = tvaRepository
            .findAllByEndEvolutionDateNotNullAndEvolutionAppliedTrueAndEndEvolutionDateLessThanEqual(today);

        for (Tva tva : toRevert) {
            // Revert to previous rate
            if (tva.getPreviousRate() != null) {
                tva.setDefaultRate(tva.getPreviousRate());
            }
            // Clear evolution fields
            tva.setPreviousRate(null);
            tva.setFutureRate(null);
            tva.setStartEvolutionDate(null);
            tva.setEndEvolutionDate(null);
            tva.setEvolutionApplied(false);
            tvaRepository.save(tva);
        }
    }
}
