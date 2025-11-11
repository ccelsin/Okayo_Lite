package backend.services.paymentdetails;

import java.util.List;

import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;

import backend.dtos.PaymentDetailsDto;
import backend.dtos.PaymentDetailsRequest;
import backend.models.PaymentDetails;
import backend.models.User;
import backend.repositories.PaymentDetailsRepository;
import backend.repositories.UserRepository;
import backend.utilities.BeanCopyUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Service applicatif responsable de la gestion des informations de paiement ({@link PaymentDetails}).
 *
 * <p>Ce service assure la création, la consultation et la mise à jour des informations de paiement
 * associées à un utilisateur.</p>
 *
 * <p>Annoté avec {@link Service} pour être géré par Spring et {@link RequiredArgsConstructor}
 * pour l’injection automatique des dépendances.</p>
 */
@Service
@RequiredArgsConstructor
public class PaymentDetailsService {

    /** Dépôt JPA pour la gestion des entités {@link PaymentDetails}. */
    private final PaymentDetailsRepository paymentDetailsRepository;

    /** Dépôt JPA pour la gestion des entités {@link User}. */
    private final UserRepository userRepository;

    /**
     * Enregistre de nouvelles informations de paiement pour un utilisateur.
     *
     * <p>Les données fournies sont d’abord normalisées dans un {@link PaymentDetailsDto}
     * avant d’être converties en entité et sauvegardées en base.</p>
     *
     * @param userId l’identifiant de l’utilisateur auquel associer les informations
     * @param paymentDetailsRequest l’objet contenant les données du moyen de paiement
     * @return les informations de paiement enregistrées sous forme de {@link PaymentDetailsDto}
     * @throws EntityNotFoundException si l’utilisateur correspondant à l’ID est introuvable
     */
    public PaymentDetailsDto savePaymentDetails(Long userId, PaymentDetailsRequest paymentDetailsRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));

        PaymentDetailsDto normalizedDto = new PaymentDetailsDto(
            null,
            paymentDetailsRequest.paymentName(),
            paymentDetailsRequest.paymentTerm(),
            paymentDetailsRequest.domiciliation(),
            paymentDetailsRequest.holderName(),
            paymentDetailsRequest.iban(),
            paymentDetailsRequest.bic(),
            userId
        );

        PaymentDetails paymentDetails = PaymentDetailsMapperService.toEntity(normalizedDto, user);
        PaymentDetails savedPaymentDetails = paymentDetailsRepository.save(paymentDetails);
        return PaymentDetailsMapperService.toDto(savedPaymentDetails);
    }

    /**
     * Récupère une information de paiement par son identifiant.
     *
     * @param id l’identifiant de l’information de paiement
     * @return l’objet {@link PaymentDetailsDto} correspondant, ou {@code null} s’il n’existe pas
     * @throws BadRequestException si l’identifiant est {@code null} ou invalide
     */
    public PaymentDetailsDto getPaymentDetails(Long id) throws BadRequestException {
        if (id == null) {
            throw new BadRequestException("Veuillez entrer une valeur valide");
        }
        return paymentDetailsRepository.findById(id)
            .map(PaymentDetailsMapperService::toDto)
            .orElse(null);
    }

    /**
     * Récupère toutes les informations de paiement enregistrées.
     *
     * @return une liste d’objets {@link PaymentDetailsDto} représentant toutes les informations enregistrées
     */
    public List<PaymentDetailsDto> getAllPaymentDetails() {
        List<PaymentDetails> paymentDetailsList = paymentDetailsRepository.findAll();
        return PaymentDetailsMapperService.toDtoList(paymentDetailsList);
    }

    /**
     * Met à jour des informations de paiement existantes tout en conservant le lien avec l’utilisateur.
     *
     * <p>Les champs non nuls du DTO remplacent ceux de l’entité existante.
     * La relation utilisateur est également revalidée.</p>
     *
     * @param id l’identifiant de l’utilisateur lié à l’information de paiement
     * @param paymentDetailsDto les nouvelles données à appliquer
     * @return les informations de paiement mises à jour sous forme de {@link PaymentDetailsDto}
     * @throws EntityNotFoundException si l’utilisateur ou l’information de paiement est introuvable
     */
    public PaymentDetailsDto setPaymentDetails(Long id, PaymentDetailsDto paymentDetailsDto) {
        User userFound = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));

        PaymentDetails paymentDetails = paymentDetailsRepository.findById(paymentDetailsDto.id())
                .orElseThrow(() -> new EntityNotFoundException("Information de paiement introuvable"));
            
        // Copie des propriétés non nulles du DTO vers l’entité existante
        BeanCopyUtils.copyNonNullProperties(paymentDetailsDto, paymentDetails);

        // Mise à jour de la relation avec l’utilisateur
        paymentDetails.setUser(userFound);
        
        PaymentDetails savedPaymentDetails = paymentDetailsRepository.save(paymentDetails);
        return PaymentDetailsMapperService.toDto(savedPaymentDetails);
    }
}
