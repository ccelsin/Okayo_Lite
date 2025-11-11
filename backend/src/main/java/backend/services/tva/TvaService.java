package backend.services.tva;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import backend.dtos.TvaDto;
import backend.models.Tva;
import backend.repositories.TvaRepository;
import backend.utilities.BeanCopyUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Service applicatif pour la gestion des taux de TVA.
 *
 * <p>Ce service centralise la logique métier liée aux entités {@link Tva}, incluant :</p>
 * <ul>
 *   <li>la création et la mise à jour des taux de TVA,</li>
 *   <li>le suivi des évolutions programmées (taux futurs et dates d’application),</li>
 *   <li>la conversion entre entités et DTOs via {@link TvaMapperService}.</li>
 * </ul>
 *
 * <p>Annoté avec {@link Service} pour la gestion Spring et {@link RequiredArgsConstructor}
 * pour l’injection automatique des dépendances par constructeur.</p>
 */
@Service
@RequiredArgsConstructor
public class TvaService {

    /** Dépôt d’accès aux entités {@link Tva}. */
    private final TvaRepository tvaRepository;

    /**
     * Récupère une TVA spécifique à partir de son identifiant.
     *
     * @param id identifiant de la TVA recherchée
     * @return un {@link TvaDto} correspondant si trouvé, sinon {@code null}
     */
    public TvaDto getTva(Long id) {
        Optional<Tva> optionalTva = tvaRepository.findById(id);
        if (optionalTva.isPresent()) {
            Tva tva = optionalTva.get();
            return TvaMapperService.toDto(tva);
        } 
        return null;
    }

    /**
     * Crée une nouvelle TVA dans la base de données après validation.
     *
     * <p>Vérifie que le taux par défaut est renseigné, puis sauvegarde la TVA.</p>
     *
     * @param tva les informations de TVA à enregistrer
     * @return la TVA créée sous forme de {@link TvaDto}
     * @throws BadRequestException si le taux par défaut est manquant ou invalide
     */
    public TvaDto saveTva(TvaDto tva) throws BadRequestException {
        TvaDto tvaUpdated = new TvaDto(null, tva.previousRate(), tva.defaultRate(), tva.futureRate(),
                                       tva.startEvolutionDate(), tva.endEvolutionDate());
        if (tvaUpdated.defaultRate() == null) {
            throw new BadRequestException("Veuillez entrer une valeur par défaut de Tva valide");
        }
        Tva tvaEntity = tvaRepository.save(TvaMapperService.toEntity(tvaUpdated));
        return TvaMapperService.toDto(tvaEntity);
    }

    /**
     * Met à jour une TVA existante en appliquant les règles d’évolution de taux.
     *
     * <p>Cette méthode est transactionnelle afin d’assurer la cohérence des données lors
     * de la mise à jour. Les transitions de taux sont gérées automatiquement selon
     * les dates définies et le statut d’évolution.</p>
     *
     * <p>Règles principales :</p>
     * <ul>
     *   <li>Si un taux futur existe et que la date de début d’évolution est atteinte,
     *       le taux futur devient le taux par défaut, et le précédent devient l’ancien taux.</li>
     *   <li>Si une évolution est appliquée et que la date de fin d’évolution est atteinte,
     *       le taux précédent redevient le taux par défaut, et les champs d’évolution sont remis à zéro.</li>
     * </ul>
     *
     * @param tvaDetails les détails de TVA à mettre à jour
     * @return la TVA mise à jour sous forme de {@link TvaDto}
     * @throws EntityNotFoundException si la TVA à modifier est introuvable
     */
    @Transactional
    public TvaDto setTva(TvaDto tvaDetails) {
        return tvaRepository.findById(tvaDetails.id()).map(tva -> {
            // Conversion du DTO en entité pour copie partielle
            Tva updates = TvaMapperService.toEntity(tvaDetails);
            BeanCopyUtils.copyNonNullProperties(updates, tva, "id", "evolutionApplied", "products");

            // Récupération de la date courante
            Date now = new Date(System.currentTimeMillis());
            Date startDate = tva.getStartEvolutionDate();
            Date endDate = tva.getEndEvolutionDate();
            boolean hasFutureRate = tva.getFutureRate() != null;
            boolean evolutionApplied = Boolean.TRUE.equals(tva.getEvolutionApplied());

            // Gestion de l’évolution automatique des taux
            if (hasFutureRate) {
                // Application du futur taux dès que la date de début est atteinte
                if (startDate != null && !startDate.after(now)) {
                    if (tva.getDefaultRate() != null) {
                        tva.setPreviousRate(tva.getDefaultRate());
                    }
                    tva.setDefaultRate(tva.getFutureRate());
                    tva.setFutureRate(null);
                    tva.setEvolutionApplied(true);
                } else {
                    tva.setEvolutionApplied(false);
                }
            } else if (evolutionApplied) {
                // Rétrogradation vers l’ancien taux lorsque la période d’évolution est terminée
                if (endDate != null && !endDate.after(now)) {
                    if (tva.getPreviousRate() != null) {
                        tva.setDefaultRate(tva.getPreviousRate());
                    }
                    tva.setPreviousRate(null);
                    tva.setStartEvolutionDate(null);
                    tva.setEndEvolutionDate(null);
                    tva.setEvolutionApplied(false);
                }
            }

            Tva tvaEntity = tvaRepository.save(tva);
            return TvaMapperService.toDto(tvaEntity);
        }).orElseThrow(() -> new EntityNotFoundException("La tva demandée n'a pas été retrouvée"));
    }

    /**
     * Récupère toutes les TVA enregistrées dans la base.
     *
     * @return une liste de {@link TvaDto} représentant toutes les TVA existantes
     */
    public List<TvaDto> getAllTva() {
        List<Tva> tvaList = tvaRepository.findAll();
        return TvaMapperService.toDtoList(tvaList);
    }
}
