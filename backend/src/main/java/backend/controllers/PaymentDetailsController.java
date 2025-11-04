package backend.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import backend.dtos.PaymentDetailsDto;
import backend.dtos.PaymentDetailsRequest;
import backend.services.paymentdetails.PaymentDetailsService;
import backend.services.user.UserService;
import backend.utilities.ResponseUtils;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment-details")
public class PaymentDetailsController {

    private final PaymentDetailsService paymentDetailsService;
    private final UserService userService;

    // Create payment details when the user has admin rights.
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<?> savePaymentDetails(HttpServletRequest request, @RequestBody PaymentDetailsRequest paymentDetailsRequest) {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.forbidden("Only admins can create payment method details");
        }

        Long userId = userService.extractUserIdFromRequest(request);
        PaymentDetailsDto savedPaymentDetails = paymentDetailsService.savePaymentDetails(userId, paymentDetailsRequest);
        return ResponseEntity.ok(savedPaymentDetails);
    }

    // List every payment details entry for an authorized user.
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<?> getAllPaymentDetails(HttpServletRequest request) {
        if (userService.isAuthorized(request) == false) {
            return ResponseUtils.unauthorized("Access denied");
        }
        List<PaymentDetailsDto> paymentDetails = paymentDetailsService.getAllPaymentDetails();
        return ResponseEntity.ok(paymentDetails);
    }

    // Load one payment details entry by its identifier.
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<?> getPaymentDetails(@PathVariable Long id) {
        PaymentDetailsDto paymentDetails = paymentDetailsService.getPaymentDetails(id);
        if (paymentDetails == null) {
            return ResponseUtils.unauthorized("Access denied");
        }
        return ResponseEntity.ok(paymentDetails);
    }

    // Update payment details when the user has admin rights.
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping
    public ResponseEntity<?> setPaymentDetails(HttpServletRequest request, @RequestBody PaymentDetailsDto paymentDetailsDto) {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.forbidden("Access denied");
        }

        Long userId = userService.extractUserIdFromRequest(request);
        PaymentDetailsDto updatedPaymentDetails = paymentDetailsService.setPaymentDetails(userId, paymentDetailsDto);
        if (updatedPaymentDetails == null) {
            return ResponseUtils.badRequest("These payment details do not exist");
        }
        return ResponseEntity.ok(updatedPaymentDetails);
    }
}