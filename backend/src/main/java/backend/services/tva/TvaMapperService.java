package backend.services.tva;

import java.util.List;

import org.springframework.stereotype.Service;

import backend.dtos.TvaDto;
import backend.models.Tva;

/**
 * Service utilitaire de mappage entre les entités {@link Tva}
 * et leurs représentations de transfert de données {@link TvaDto}.
 *
 * <p>Ce service centralise la logique de conversion afin de séparer les
 * préoccupations entre la couche métier (entités JPA) et la couche de transport
 * (DTOs) utilisée dans les contrôleurs ou les services.</p>
 *
 * <p>Annoté avec {@link Service} pour permettre son injection par Spring.</p>
 */
@Service
public class TvaMapperService {

    /**
     * Convertit une entité {@link Tva} en un objet {@link TvaDto}.
     *
     * @param tva l’entité TVA à convertir
     * @return un {@link TvaDto} contenant les informations de taux et d’évolution
     */
    public static TvaDto toDto(Tva tva) {
        return new TvaDto(
            tva.getId(),
            tva.getPreviousRate(),
            tva.getDefaultRate(),
            tva.getFutureRate(),
            tva.getStartEvolutionDate(),
            tva.getEndEvolutionDate()
        );
    }

    /**
     * Convertit un objet {@link TvaDto} en une entité {@link Tva}.
     *
     * @param tvaDto le DTO TVA à convertir
     * @return une nouvelle instance de {@link Tva} remplie à partir du DTO
     */
    public static Tva toEntity(TvaDto tvaDto) {
        Tva tva = new Tva();
        tva.setId(tvaDto.id());
        tva.setPreviousRate(tvaDto.previousRate());
        tva.setDefaultRate(tvaDto.defaultRate());
        tva.setFutureRate(tvaDto.futureRate());
        tva.setStartEvolutionDate(tvaDto.startEvolutionDate());
        tva.setEndEvolutionDate(tvaDto.endEvolutionDate());
        return tva;
    }

    /**
     * Convertit une liste d’entités {@link Tva} en une liste de {@link TvaDto}.
     *
     * @param tvaList liste d’entités TVA
     * @return liste correspondante de {@link TvaDto}
     */
    public static List<TvaDto> toDtoList(List<Tva> tvaList) {
        java.util.List<TvaDto> tvaDtoList = new java.util.ArrayList<>();
        for (Tva tva : tvaList) {
            tvaDtoList.add(toDto(tva));
        }
        return tvaDtoList;
    }
}
