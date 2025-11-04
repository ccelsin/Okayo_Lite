package backend.services.tva;


import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import backend.dtos.TvaDto;
import backend.models.Tva;
import backend.repositories.TvaRepository;
import backend.utilities.BeanCopyUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TvaService {

    private final TvaRepository tvaRepository;

    public TvaDto getTva(Long id) {
        Optional<Tva> optionalTva = tvaRepository.findById(id);
        if (optionalTva.isPresent()) {
            Tva tva = optionalTva.get();
            return TvaMapperService.toDto(tva);
        } else {
            return null;
        }
        
    }

    public TvaDto saveTva(TvaDto tva) {
        TvaDto tvaUpdated = new TvaDto(null, tva.previousRate(),tva.defaultRate(), tva.futureRate(), tva.startEvolutionDate(),tva.endEvolutionDate());
        Tva tvaEntity = tvaRepository.save(TvaMapperService.toEntity(tvaUpdated));
        return TvaMapperService.toDto(tvaEntity);
    }

    @Transactional
     public TvaDto setTva(TvaDto tvaDetails) {
        return tvaRepository.findById(tvaDetails.id()).map(tva -> {
            // Update fields
            Tva updates = TvaMapperService.toEntity(tvaDetails);
            BeanCopyUtils.copyNonNullProperties(updates, tva, "id", "evolutionApplied", "products");

            
            Date now = new Date(System.currentTimeMillis());

            Date startDate = tva.getStartEvolutionDate();
            Date endDate = tva.getEndEvolutionDate();
            boolean hasFutureRate = tva.getFutureRate() != null;
            boolean evolutionApplied = Boolean.TRUE.equals(tva.getEvolutionApplied());

            if (hasFutureRate) {
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
        }).orElseThrow(() -> new EntityNotFoundException("Tva not found"));
}   


    public String deleteTva(Long id) {
        tvaRepository.deleteById(id);
        return "Tva with id " + id + " has been deleted.";
    }

    public List<TvaDto> getAllTva() {
        List<Tva> tvaList = tvaRepository.findAll();
        return TvaMapperService.toDtoList(tvaList);
    }
    
}
