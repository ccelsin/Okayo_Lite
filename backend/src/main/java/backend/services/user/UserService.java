package backend.services.user;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import backend.configuration.JwtUtils;
import backend.constants.UserRole;
import backend.dtos.UserDto;
import backend.models.User;
import backend.repositories.UserRepository;
import backend.utilities.BeanCopyUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    
    
    public UserDto getProfile(Long userId) {
        return userRepository.findById(userId)
                .map(UserMapperService::toDto)
                .orElse(null);
    }
    
    public Long extractUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        return extractUserIdFromToken(token);
    }

    public Long extractUserIdFromToken(String token) {
        String username = jwtUtils.extractUsername(token);
            User user = userRepository.findByUsername(username);
            if (user != null) {
                return Long.valueOf(user.getId());
            }
        return null; 
    }

    public boolean isAuthorized(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authHeader.substring(7);
        return !jwtUtils.isTokenExpired(token);
    }

    public boolean isAdmin(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authHeader.substring(7);
        String username = jwtUtils.extractUsername(token);
        User user = userRepository.findByUsername(username);
        return user != null && user.getRole() == UserRole.ADMIN;
    }

    public UserDto setProfile(Long userId, UserDto userDto) {
        User check = userRepository.findByUsername(userDto.username());
        if(check != null){
            new DataIntegrityViolationException("Username already taken");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User details not found"));
        
        BeanCopyUtils.copyNonNullProperties(userDto, user);
        user.setId(userId);
        User savedUser = userRepository.save(user);
        return UserMapperService.toDto(savedUser);
    }

    public UserDto getUser(Long id) {
        Optional<User> user = userRepository.findById(id);
        return user.map(UserMapperService::toDto).orElse(null);
    }

    public List<UserDto> getAllUser() {
        List<User> users = userRepository.findAll();
        return UserMapperService.toDtoList(users);
    }

     public boolean isCustomer(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authHeader.substring(7);
        String username = jwtUtils.extractUsername(token);
        User user = userRepository.findByUsername(username);
        return user != null && user.getRole() == UserRole.CUSTOMER;
    }

    public String generateCode() {
        Random random = new Random();

        // Create random nomber
        String part1 = String.format("%04d", random.nextInt(10000)); // 0000 → 9999
        String part2 = String.format("%04d", random.nextInt(10000)); // 0000 → 9999

        // Build the final code
        String code = "CU" + part1 + "-" + part2;

        // Check if this code already exists
        Optional <User> customerOpt = userRepository.findByCodeCustomer(code);
        if(customerOpt.isPresent())
        {
            // Regenarate the code it already exists
                return generateCode();
        }
            return code;
    
    }
    
}
