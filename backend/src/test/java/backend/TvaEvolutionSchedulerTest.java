package backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import backend.models.Tva;
import backend.repositories.TvaRepository;
import backend.services.tva.TvaEvolutionScheduler;

/**
 * Tests unitaires du planificateur {@link TvaEvolutionScheduler}.
 *
 * Objectifs :
 *  - Vérifier l’application des évolutions (start date atteinte) :
 *      previousRate <- defaultRate, defaultRate <- futureRate, evolutionApplied = true
 *      (remarque : le scheduler NE remet PAS futureRate à null dans l’implémentation actuelle)
 *  - Vérifier la réversion (end date atteinte) :
 *      defaultRate <- previousRate, reset des champs d’évolution, evolutionApplied = false
 *  - Vérifier le comportement “no-op” quand aucune entité n’est à traiter
 *
 * Stratégie :
 *  - On mock le repository pour retourner des listes contrôlées.
 *  - On vérifie les mutations sur les entités et les appels à save(...).
 *  - Aucun contexte Spring n’est démarré : tests rapides et isolés.
 */
@ExtendWith(MockitoExtension.class)
class TvaEvolutionSchedulerTest {

    @Mock
    private TvaRepository tvaRepository;

    @InjectMocks
    private TvaEvolutionScheduler scheduler;

    @Test
    @DisplayName("applyPlannedEvolutions : applique les taux futurs quand la date de début est atteinte")
    void applyPlannedEvolutions_applique_evolutions_quand_start_atteinte() {
        // Arrange
        Date nowMinus = new Date(System.currentTimeMillis() - 1000); // start déjà atteint
        Tva toApply = new Tva();
        toApply.setId(1L);
        toApply.setDefaultRate(new BigDecimal("20.00"));
        toApply.setFutureRate(new BigDecimal("22.00"));
        toApply.setStartEvolutionDate(nowMinus);
        toApply.setEvolutionApplied(Boolean.FALSE);

        // Le scheduler interroge d'abord la liste "toApply", puis la liste "toRevert"
        when(tvaRepository.findAllByStartEvolutionDateNotNullAndFutureRateNotNullAndEvolutionAppliedFalseAndStartEvolutionDateLessThanEqual(any(Date.class)))
                .thenReturn(List.of(toApply));
        when(tvaRepository.findAllByEndEvolutionDateNotNullAndEvolutionAppliedTrueAndEndEvolutionDateLessThanEqual(any(Date.class)))
                .thenReturn(List.of());

        // On renvoie l'entité telle quelle lors du save
        when(tvaRepository.save(any(Tva.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        scheduler.applyPlannedEvolutions();

        // Assert
        // previousRate doit recevoir l'ancien defaultRate
        assertThat(toApply.getPreviousRate()).isEqualByComparingTo("20.00");
        // defaultRate devient futureRate
        assertThat(toApply.getDefaultRate()).isEqualByComparingTo("22.00");
        // evolutionApplied devient true
        assertThat(toApply.getEvolutionApplied()).isTrue();
        // L’implémentation actuelle du scheduler NE remet pas futureRate à null (on vérifie donc qu’il est resté)
        assertThat(toApply.getFutureRate()).isEqualByComparingTo("22.00");

        // Vérifie les interactions
        verify(tvaRepository, times(1))
                .findAllByStartEvolutionDateNotNullAndFutureRateNotNullAndEvolutionAppliedFalseAndStartEvolutionDateLessThanEqual(any(Date.class));
        verify(tvaRepository, times(1)).save(toApply);
        verify(tvaRepository, times(1))
                .findAllByEndEvolutionDateNotNullAndEvolutionAppliedTrueAndEndEvolutionDateLessThanEqual(any(Date.class));
        verifyNoMoreInteractions(tvaRepository);
    }

    @Test
    @DisplayName("applyPlannedEvolutions : revient à l'ancien taux quand la date de fin est atteinte")
    void applyPlannedEvolutions_revient_previous_quand_end_atteinte() {
        // Arrange
        Date nowMinus = new Date(System.currentTimeMillis() - 1000); // fin déjà atteinte
        Tva toRevert = new Tva();
        toRevert.setId(2L);
        toRevert.setPreviousRate(new BigDecimal("10.00"));
        toRevert.setDefaultRate(new BigDecimal("12.00"));
        toRevert.setEndEvolutionDate(nowMinus);
        toRevert.setEvolutionApplied(Boolean.TRUE);

        when(tvaRepository.findAllByStartEvolutionDateNotNullAndFutureRateNotNullAndEvolutionAppliedFalseAndStartEvolutionDateLessThanEqual(any(Date.class)))
                .thenReturn(List.of());
        when(tvaRepository.findAllByEndEvolutionDateNotNullAndEvolutionAppliedTrueAndEndEvolutionDateLessThanEqual(any(Date.class)))
                .thenReturn(List.of(toRevert));
        when(tvaRepository.save(any(Tva.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        scheduler.applyPlannedEvolutions();

        // Assert
        // defaultRate reprend previousRate
        assertThat(toRevert.getDefaultRate()).isEqualByComparingTo("10.00");
        // Tous les champs d’évolution sont réinitialisés
        assertThat(toRevert.getPreviousRate()).isNull();
        assertThat(toRevert.getFutureRate()).isNull();
        assertThat(toRevert.getStartEvolutionDate()).isNull();
        assertThat(toRevert.getEndEvolutionDate()).isNull();
        assertThat(toRevert.getEvolutionApplied()).isFalse();

        // Vérifie les interactions
        verify(tvaRepository, times(1))
                .findAllByStartEvolutionDateNotNullAndFutureRateNotNullAndEvolutionAppliedFalseAndStartEvolutionDateLessThanEqual(any(Date.class));
        verify(tvaRepository, times(1))
                .findAllByEndEvolutionDateNotNullAndEvolutionAppliedTrueAndEndEvolutionDateLessThanEqual(any(Date.class));
        verify(tvaRepository, times(1)).save(toRevert);
        verifyNoMoreInteractions(tvaRepository);
    }

    @Test
    @DisplayName("applyPlannedEvolutions : ne fait rien quand aucune entité n'est à traiter")
    void applyPlannedEvolutions_noop_quand_listes_vides() {
        // Arrange
        when(tvaRepository.findAllByStartEvolutionDateNotNullAndFutureRateNotNullAndEvolutionAppliedFalseAndStartEvolutionDateLessThanEqual(any(Date.class)))
                .thenReturn(List.of());
        when(tvaRepository.findAllByEndEvolutionDateNotNullAndEvolutionAppliedTrueAndEndEvolutionDateLessThanEqual(any(Date.class)))
                .thenReturn(List.of());

        // Act
        scheduler.applyPlannedEvolutions();

        // Assert
        verify(tvaRepository, times(1))
                .findAllByStartEvolutionDateNotNullAndFutureRateNotNullAndEvolutionAppliedFalseAndStartEvolutionDateLessThanEqual(any(Date.class));
        verify(tvaRepository, times(1))
                .findAllByEndEvolutionDateNotNullAndEvolutionAppliedTrueAndEndEvolutionDateLessThanEqual(any(Date.class));
        verify(tvaRepository, never()).save(any(Tva.class));
        verifyNoMoreInteractions(tvaRepository);
    }
}
