package backend.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import backend.configuration.JwtUtils;
import backend.constants.UserRole;
import backend.dtos.AuthUserDto;
import backend.dtos.RegisterDto;
import backend.models.User;
import backend.repositories.UserRepository;
import backend.services.user.UserService;
import backend.utilities.ResponseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    

    @PostMapping("/register")
    public ResponseEntity <?> register(@RequestBody RegisterDto authUserDto) {
        try {
            if (userRepository.findByUsername(authUserDto.username()) != null) {
                return ResponseUtils.conflict("Username is already taken");
            }
            User user = new User();
            user.setUsername(authUserDto.username());
            user.setPassword(passwordEncoder.encode(authUserDto.password()));
            UserRole role = authUserDto.role() != null ? authUserDto.role() : UserRole.ADMIN;
            user.setRole(role);
            if (role == UserRole.CUSTOMER) {
                String code = userService.generateCode();
                user.setCodeCustomer(code);
            }
            userRepository.save(user);
            return ResponseEntity.ok(authUserDto);
        } catch (Exception e) {
            log.error("Failed to register user", e);
            return ResponseUtils.internalServerError("Failed to register user");
        }
        
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthUserDto authUserDto) {

        try{
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authUserDto.username(), authUserDto.password()));
            if (authentication.isAuthenticated()) {
                Map<String, Object> authData = new HashMap<>();
                authData.put("token", jwtUtils.generateToken(authUserDto.username()));
                authData.put("type", "Bearer");
                return ResponseEntity.ok(authData);
            } 
                return ResponseUtils.unauthorized("Invalid username or password");

        } catch (AuthenticationException e) {
            log.error(e.getMessage());
            return ResponseUtils.unauthorized("Invalid username or password");
        }
    }
    
}
