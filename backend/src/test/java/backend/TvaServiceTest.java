package backend;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import backend.dtos.TvaDto;
import backend.models.Tva;
import backend.repositories.TvaRepository;
import backend.services.tva.TvaService;
import jakarta.persistence.EntityNotFoundException;

/**
 * Tests unitaires de {@link TvaService}.
 *
 * Stratégie :
 *  - getTva : retourne le DTO si présent, sinon null.
 *  - saveTva : valide le defaultRate, persiste et retourne le DTO sauvegardé.
 *  - setTva : applique les règles d’évolution (application du futur taux, ou retour à l’ancien),
 *             et lève une EntityNotFoundException si l’ID n’existe pas.
 *  - getAllTva : renvoie la liste mappée.
 *
 * Remarques :
 *  - Aucun contexte Spring n’est démarré.
 *  - On contrôle les appels au dépôt via Mockito et on vérifie les champs attendus.
 */
@ExtendWith(MockitoExtension.class)
class TvaServiceTest {

    @Mock
    private TvaRepository tvaRepository;

    @InjectMocks
    private TvaService service;

    // ---------------------------
    // getTva
    // ---------------------------
    @Test
    @DisplayName("getTva : retourne le DTO quand l'entité existe")
    void getTva_returnsDto_whenFound() {
        Tva entity = new Tva();
        entity.setId(1L);
        entity.setDefaultRate(new BigDecimal("20.00"));
        when(tvaRepository.findById(1L)).thenReturn(Optional.of(entity));

        TvaDto dto = service.getTva(1L);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.defaultRate()).isEqualByComparingTo("20.00");
        verify(tvaRepository).findById(1L);
    }

    @Test
    @DisplayName("getTva : retourne null quand l'entité est introuvable")
    void getTva_returnsNull_whenNotFound() {
        when(tvaRepository.findById(99L)).thenReturn(Optional.empty());

        TvaDto dto = service.getTva(99L);

        assertThat(dto).isNull();
        verify(tvaRepository).findById(99L);
    }

    // ---------------------------
    // saveTva
    // ---------------------------
    @Test
    @DisplayName("saveTva : persiste et retourne le DTO quand defaultRate est fourni")
    void saveTva_persists_whenDefaultRateProvided() throws Exception {
        TvaDto input = new TvaDto(
            null,
            null,
            new BigDecimal("10.00"),
            null,
            null,
            null
        );

        // On capture l'entité passée au dépôt pour vérifier les champs
        ArgumentCaptor<Tva> captor = ArgumentCaptor.forClass(Tva.class);
        Tva saved = new Tva();
        saved.setId(7L);
        saved.setDefaultRate(new BigDecimal("10.00"));
        when(tvaRepository.save(any(Tva.class))).thenReturn(saved);

        TvaDto out = service.saveTva(input);

        verify(tvaRepository).save(captor.capture());
        Tva persisted = captor.getValue();
        assertThat(persisted.getId()).isNull();
        assertThat(persisted.getDefaultRate()).isEqualByComparingTo("10.00");

        assertThat(out.id()).isEqualTo(7L);
        assertThat(out.defaultRate()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("saveTva : lève BadRequestException quand defaultRate est null")
    void saveTva_throws_whenDefaultRateNull() {
        TvaDto input = new TvaDto(
            null,
            null,
            null, // defaultRate manquant
            null,
            null,
            null
        );

        assertThatThrownBy(() -> service.saveTva(input))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("valeur par défaut");

        verifyNoInteractions(tvaRepository);
    }

    // ---------------------------
    // setTva : règles d’évolution
    // ---------------------------
    @Nested
    @DisplayName("setTva : gestion des évolutions")
    class SetTvaEvolution {

        @Test
        @DisplayName("applique le futur taux si la date de début est atteinte ou passée")
        void appliesFutureRate_whenStartDateReached() {
            // Entité existante avec un taux par défaut et un futur taux
            Tva existing = new Tva();
            existing.setId(10L);
            existing.setDefaultRate(new BigDecimal("20.00"));
            existing.setFutureRate(new BigDecimal("22.00"));
            existing.setStartEvolutionDate(new Date(System.currentTimeMillis() - 1000)); // déjà atteint
            existing.setEvolutionApplied(Boolean.FALSE);

            when(tvaRepository.findById(10L)).thenReturn(Optional.of(existing));
            when(tvaRepository.save(any(Tva.class))).thenAnswer(inv -> inv.getArgument(0));

            // DTO de mise à jour : on peut laisser la plupart des champs null,
            // BeanCopyUtils copiera seulement ceux non null (sauf 'id' ignoré ici par le service)
            TvaDto update = new TvaDto(
                10L,
                null,                // previousRate (non forcé)
                null,                // defaultRate (non forcé)
                null,                // futureRate (non forcé)
                existing.getStartEvolutionDate(),
                null
            );

            TvaDto out = service.setTva(update);

            // Après application :
            assertThat(out.previousRate()).isEqualByComparingTo("20.00"); // l'ancien défaut devient previous
            assertThat(out.defaultRate()).isEqualByComparingTo("22.00");  // le futur devient défaut
            assertThat(out.futureRate()).isNull();                        // futur consommé
            assertThat(existing.getEvolutionApplied()).isTrue();

            verify(tvaRepository).findById(10L);
            verify(tvaRepository).save(existing);
        }

        @Test
        @DisplayName("n'applique rien si futur taux existe mais que la date de début n'est pas encore atteinte")
        void doesNotApplyFutureRate_whenStartDateNotReached() {
            Tva existing = new Tva();
            existing.setId(11L);
            existing.setDefaultRate(new BigDecimal("5.50"));
            existing.setFutureRate(new BigDecimal("7.00"));
            existing.setStartEvolutionDate(new Date(System.currentTimeMillis() + 86_400_000)); // demain
            existing.setEvolutionApplied(Boolean.FALSE);

            when(tvaRepository.findById(11L)).thenReturn(Optional.of(existing));
            when(tvaRepository.save(any(Tva.class))).thenAnswer(inv -> inv.getArgument(0));

            TvaDto update = new TvaDto(11L, null, null, null, existing.getStartEvolutionDate(), null);

            TvaDto out = service.setTva(update);

            // Rien ne change, evolutionApplied reste false
            assertThat(out.defaultRate()).isEqualByComparingTo("5.50");
            assertThat(out.futureRate()).isEqualByComparingTo("7.00");
            assertThat(existing.getEvolutionApplied()).isFalse();

            verify(tvaRepository).findById(11L);
            verify(tvaRepository).save(existing);
        }

        @Test
        @DisplayName("revient au taux précédent quand l'évolution est appliquée et la date de fin atteinte")
        void revertsToPrevious_whenEndDateReached() {
            Tva existing = new Tva();
            existing.setId(12L);
            existing.setPreviousRate(new BigDecimal("10.00"));
            existing.setDefaultRate(new BigDecimal("12.00")); // taux en vigueur actuellement
            existing.setFutureRate(null);                     // pas de futur taux
            existing.setStartEvolutionDate(new Date(System.currentTimeMillis() - 86_400_000)); // hier
            existing.setEndEvolutionDate(new Date(System.currentTimeMillis() - 1000));         // passé
            existing.setEvolutionApplied(Boolean.TRUE);       // évolution active

            when(tvaRepository.findById(12L)).thenReturn(Optional.of(existing));
            when(tvaRepository.save(any(Tva.class))).thenAnswer(inv -> inv.getArgument(0));

            TvaDto update = new TvaDto(
                12L,
                existing.getPreviousRate(),
                existing.getDefaultRate(),
                null,
                existing.getStartEvolutionDate(),
                existing.getEndEvolutionDate()
            );

            TvaDto out = service.setTva(update);

            // Le previous redevient default, et les champs d'évolution sont nettoyés
            assertThat(out.defaultRate()).isEqualByComparingTo("10.00");
            assertThat(out.previousRate()).isNull();
            assertThat(out.futureRate()).isNull();
            assertThat(out.startEvolutionDate()).isNull();
            assertThat(out.endEvolutionDate()).isNull();
            assertThat(existing.getEvolutionApplied()).isFalse();

            verify(tvaRepository).findById(12L);
            verify(tvaRepository).save(existing);
        }

        @Test
        @DisplayName("lève EntityNotFoundException quand l'ID n'existe pas")
        void throws_whenIdNotFound() {
            when(tvaRepository.findById(404L)).thenReturn(Optional.empty());

            TvaDto update = new TvaDto(404L, null, new BigDecimal("20.00"), null, null, null);

            assertThatThrownBy(() -> service.setTva(update))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("tva")
                .hasMessageContaining("retrouv");
        }
    }

    // ---------------------------
    // getAllTva
    // ---------------------------
    @Test
    @DisplayName("getAllTva : retourne la liste des DTO mappés")
    void getAllTva_returnsList() {
        Tva t1 = new Tva(); t1.setId(1L); t1.setDefaultRate(new BigDecimal("5.50"));
        Tva t2 = new Tva(); t2.setId(2L); t2.setDefaultRate(new BigDecimal("20.00"));
        when(tvaRepository.findAll()).thenReturn(List.of(t1, t2));

        List<TvaDto> list = service.getAllTva();

        assertThat(list).hasSize(2);
        assertThat(list.get(0).id()).isEqualTo(1L);
        assertThat(list.get(0).defaultRate()).isEqualByComparingTo("5.50");
        assertThat(list.get(1).id()).isEqualTo(2L);
        assertThat(list.get(1).defaultRate()).isEqualByComparingTo("20.00");

        verify(tvaRepository).findAll();
    }
}
