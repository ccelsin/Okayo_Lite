package backend.services.paymentdetails;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import backend.dtos.PaymentDetailsDto;
import backend.models.PaymentDetails;
import backend.models.User;

/**
 * Service utilitaire de mappage entre les entités {@link PaymentDetails}
 * et leurs objets de transfert de données {@link PaymentDetailsDto}.
 *
 * <p>Ce service fournit des méthodes statiques permettant de convertir
 * les entités JPA en DTOs et inversement, afin de faciliter les échanges
 * entre la couche de persistance et la couche de service ou de présentation.</p>
 *
 * <p>Annoté avec {@link Service} pour une éventuelle injection Spring,
 * même si ses méthodes sont statiques.</p>
 */
@Service
public class PaymentDetailsMapperService {

    /**
     * Convertit une entité {@link PaymentDetails} en un objet {@link PaymentDetailsDto}.
     *
     * <p>Cette méthode extrait notamment l’identifiant de l’utilisateur associé
     * afin de ne pas exposer directement l’objet {@link User} dans le DTO.</p>
     *
     * @param paymentDetails l’entité {@link PaymentDetails} à convertir
     * @return une instance de {@link PaymentDetailsDto} représentant les mêmes données
     */
    public static PaymentDetailsDto toDto(PaymentDetails paymentDetails) {
        Long userId = paymentDetails.getUser() != null ? paymentDetails.getUser().getId() : null;

        return new PaymentDetailsDto(
            paymentDetails.getId(),
            paymentDetails.getPaymentName(),
            paymentDetails.getPaymentTerm(),
            paymentDetails.getDomiciliation(),
            paymentDetails.getHolderName(),
            paymentDetails.getIban(),
            paymentDetails.getBic(),
            userId
        );
    }

    /**
     * Convertit un objet {@link PaymentDetailsDto} en une entité {@link PaymentDetails}.
     *
     * <p>La relation avec l’utilisateur est fournie via le paramètre {@code user},
     * ce qui permet de maintenir la cohérence entre les entités liées.</p>
     *
     * @param paymentDetailsDto le DTO contenant les informations à convertir
     * @param user l’utilisateur associé à ces informations de paiement
     * @return une instance de {@link PaymentDetails} prête à être persistée
     */
    public static PaymentDetails toEntity(PaymentDetailsDto paymentDetailsDto, User user) {

        PaymentDetails paymentDetails = new PaymentDetails();
        paymentDetails.setId(paymentDetailsDto.id());
        paymentDetails.setPaymentName(paymentDetailsDto.paymentName());
        paymentDetails.setPaymentTerm(paymentDetailsDto.paymentTerm());
        paymentDetails.setDomiciliation(paymentDetailsDto.domiciliation());
        paymentDetails.setHolderName(paymentDetailsDto.holderName());
        paymentDetails.setIban(paymentDetailsDto.iban());
        paymentDetails.setBic(paymentDetailsDto.bic());
        paymentDetails.setUser(user);
        return paymentDetails;
    }

    /**
     * Convertit une liste d’entités {@link PaymentDetails} en une liste de {@link PaymentDetailsDto}.
     *
     * @param paymentDetailsList la liste des entités à convertir
     * @return la liste correspondante des DTOs
     */
    public static List<PaymentDetailsDto> toDtoList(List<PaymentDetails> paymentDetailsList) {
        List<PaymentDetailsDto> paymentDetailsDtos = new ArrayList<>();
        
        for (PaymentDetails paymentDetails : paymentDetailsList) {
            paymentDetailsDtos.add(toDto(paymentDetails));
        }
        return paymentDetailsDtos;
    }
}
