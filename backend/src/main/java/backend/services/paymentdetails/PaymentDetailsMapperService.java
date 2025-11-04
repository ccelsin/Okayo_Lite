package backend.services.paymentdetails;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import backend.dtos.PaymentDetailsDto;
import backend.models.PaymentDetails;
import backend.models.User;

@Service
public class PaymentDetailsMapperService {

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

    public static List<PaymentDetailsDto> toDtoList(List<PaymentDetails> paymentDetailsList) {
        List<PaymentDetailsDto> paymentDetailsDtos = new ArrayList<>();
        
        for (PaymentDetails paymentDetails : paymentDetailsList) {
            paymentDetailsDtos.add(toDto(paymentDetails));
        }
        return paymentDetailsDtos;
    }
}