package backend.services.tva;

import java.util.List;

import org.springframework.stereotype.Service;

import backend.dtos.TvaDto;
import backend.models.Tva;

@Service
public class TvaMapperService {

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

    public static List<TvaDto> toDtoList(List<Tva> tvaList) {
        java.util.List<TvaDto> tvaDtoList = new java.util.ArrayList<>();
        for (Tva tva : tvaList) {
            tvaDtoList.add(toDto(tva));
        }
        return tvaDtoList;
    }
    
}
