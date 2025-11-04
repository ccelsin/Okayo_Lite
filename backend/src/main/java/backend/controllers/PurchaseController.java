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

import backend.dtos.PurchaseDto;
import backend.services.purchase.PurchaseService;
import backend.services.user.UserService;
import backend.utilities.ResponseUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/purchase")
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final UserService userService;

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<?> savePurchase(HttpServletRequest request, @RequestBody PurchaseDto purchaseDto) {
        if (userService.isCustomer(request) == false) {
            return ResponseUtils.forbidden("Only customers can create purchases.");
        }

        Long purchaserId = userService.extractUserIdFromRequest(request);
        if (purchaserId == null) {
            return ResponseUtils.unauthorized("Access denied");
        }

        PurchaseDto sanitizedPurchase = new PurchaseDto(
            null,
            purchaseDto.productId(),
            purchaseDto.name(),
            purchaseDto.quantity(),
            purchaseDto.unitPriceHT(),
            purchaseDto.totalHT(),
            purchaseDto.tvaApplied(),
            purchaseDto.totalTva(),
            null,
            purchaserId,
            Boolean.FALSE
        );
        try {
            PurchaseDto savedPurchase = purchaseService.savePurchase(sanitizedPurchase);
            return ResponseEntity.ok(savedPurchase);
        } catch (EntityNotFoundException ex) {
            return ResponseUtils.notFound(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseUtils.conflict(ex.getMessage());
        }
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<?> getAllPurchases(HttpServletRequest request) {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.unauthorized("Access denied");
        }
        List<PurchaseDto> purchases = purchaseService.getAllPurchases();
        return ResponseEntity.ok(purchases);
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<?> getPurchase(@PathVariable Long id) {
        PurchaseDto purchase = purchaseService.getPurchase(id);
        if (purchase == null) {
            return ResponseUtils.notFound("Purchase not found with id " + id);
        }
        return ResponseEntity.ok(purchase);
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/mine")
    public ResponseEntity<?> getPurchaseByPurchaser(HttpServletRequest request) {
        Long purchaserId = userService.extractUserIdFromRequest(request);
        if (purchaserId == null) {
            return ResponseUtils.unauthorized("Access denied");
        }

        List<PurchaseDto> purchases = purchaseService.getPurchasesByPurchaser(purchaserId);
        return ResponseEntity.ok(purchases);
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}/customer")
    public ResponseEntity<?> getPurchaseOfPurchaser(@PathVariable Long id, HttpServletRequest request) {
        
        if (userService.isAdmin (request) == false) {
            return ResponseUtils.unauthorized("Access denied");
        }

        List<PurchaseDto> purchases = purchaseService.getPurchasesByPurchaser(id);
        return ResponseEntity.ok(purchases);
    }
    

    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/")
    public ResponseEntity<?> setPurchase(HttpServletRequest request, @RequestBody PurchaseDto purchaseDto) {

        if (userService.isAdmin(request) == false) {
            ResponseUtils.unauthorized("Invalid token");
        }
        
        PurchaseDto purchaseDtoUpdated = new PurchaseDto(
            purchaseDto.id(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            purchaseDto.invoiceId(),
            null,
            purchaseDto.isConfirmed()
        );

        try {
            PurchaseDto updatedPurchase = purchaseService.setPurchase(purchaseDtoUpdated);
            return ResponseEntity.ok(updatedPurchase);
        } catch (EntityNotFoundException ex) {
            return ResponseUtils.notFound(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseUtils.conflict(ex.getMessage());
        }
    }
}