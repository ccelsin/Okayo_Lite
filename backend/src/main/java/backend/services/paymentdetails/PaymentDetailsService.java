package backend.services.paymentdetails;

import java.util.List;
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

@Service
@RequiredArgsConstructor
public class PaymentDetailsService {

    private final PaymentDetailsRepository paymentDetailsRepository;
    private final UserRepository userRepository;

    // Save new payment details for a user.
    public PaymentDetailsDto savePaymentDetails(Long userId, PaymentDetailsRequest paymentDetailsRequest) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
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

    // Load one payment details by its identifier.
    public PaymentDetailsDto getPaymentDetails(Long id) {
        return paymentDetailsRepository.findById(id)
            .map(PaymentDetailsMapperService::toDto)
            .orElse(null);
    }

    // Load all payment details stored in the database.
    public List<PaymentDetailsDto> getAllPaymentDetails() {
        List<PaymentDetails> paymentDetailsList = paymentDetailsRepository.findAll();
        return PaymentDetailsMapperService.toDtoList(paymentDetailsList);
    }

    // Update payment details and keep the link with the user.
    public PaymentDetailsDto setPaymentDetails(Long id, PaymentDetailsDto paymentDetailsDto) {
        User userFound = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        PaymentDetails paymentDetails = paymentDetailsRepository.findById(paymentDetailsDto.id())
            .orElseThrow(() -> new EntityNotFoundException("Payment details not found"));
            
        BeanCopyUtils.copyNonNullProperties(paymentDetailsDto, paymentDetails);
        
        paymentDetails.setUser(userFound);
        
        PaymentDetails savedPaymentDetails = paymentDetailsRepository.save(paymentDetails);
        return PaymentDetailsMapperService.toDto(savedPaymentDetails);
    }

    
}   