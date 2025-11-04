package backend.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import backend.dtos.TvaDto;
import backend.services.tva.TvaService;
import backend.services.user.UserService;
import backend.utilities.ResponseUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tva")
public class TvaController {

    private final TvaService tvaService;
    private final UserService userService;

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<?> getAllTva(HttpServletRequest request) {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.unauthorized("Access denied");
        }
        List<TvaDto> tvaList = tvaService.getAllTva();
        return ResponseEntity.ok(tvaList);
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<?> getTva(HttpServletRequest request,@PathVariable Long id) {
        if(userService.isAdmin(request) == false) {
            return ResponseUtils.forbidden("Only admins can access TVA details.");
        }
        TvaDto tva = tvaService.getTva(id);
        return ResponseEntity.ok(tva);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<?> saveTva(HttpServletRequest request, @RequestBody TvaDto tva) {
        if(userService.isAdmin(request) == false) {
            return ResponseUtils.forbidden("Only admins can access TVA details.");
        }
        TvaDto savedTva = tvaService.saveTva(tva);
        return ResponseEntity.ok(savedTva);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PutMapping
    public ResponseEntity<?> setTva(HttpServletRequest request, @RequestBody TvaDto tvaDetails) {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.unauthorized("Access denied");
        }
        TvaDto updatedTva = tvaService.setTva(tvaDetails);
        return ResponseEntity.ok(updatedTva);
    }
    
}
