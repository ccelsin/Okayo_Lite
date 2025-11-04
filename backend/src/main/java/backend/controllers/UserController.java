package backend.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import backend.utilities.ResponseUtils;

import backend.dtos.UserDto;
import backend.models.User;
import backend.repositories.UserRepository;
import backend.services.user.UserMapperService;
import backend.services.user.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (userService.isAuthorized(request) == false) {
            return ResponseUtils.unauthorized("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        Long userId = userService.extractUserIdFromToken(token);
        if (userId == null) {
            return ResponseUtils.unauthorized("Invalid token");
        }

        var userDto = userService.getProfile(userId);
        return ResponseEntity.ok(userDto);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(HttpServletRequest request, @RequestBody UserDto updatedUserDto) {
        String authHeader = request.getHeader("Authorization");
        if (userService.isAuthorized(request) == false) {
            return ResponseUtils.unauthorized("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        Long userId = userService.extractUserIdFromToken(token);
        if (userId == null) {
            ResponseUtils.unauthorized("Invalid token");
        }
        userService.setProfile(userId, updatedUserDto);
        return ResponseEntity.status(200).body("Profile updated. You have to login now");
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        
        UserDto customer = userService.getUser(id);
        if (customer == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(customer);
    }

    public List<UserDto> getAllCustomer() {
        List<User> customers = userRepository.findAll();
        return UserMapperService.toDtoList(customers);
    }

    @GetMapping("/isAdmin")
    public boolean checkIfAdmin(HttpServletRequest request){
        return userService.isAdmin(request);
    }

    
    
}
