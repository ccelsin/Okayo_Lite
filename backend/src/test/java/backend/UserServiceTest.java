package backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import backend.configuration.JwtUtils;
import backend.constants.UserRole;
import backend.dtos.UserDto;
import backend.models.User;
import backend.repositories.UserRepository;
import backend.services.user.UserService;

import java.util.Optional;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests unitaires de UserService (extraction depuis token, rôles).
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock JwtUtils jwtUtils;

    @InjectMocks UserService userService;

    @Mock HttpServletRequest request;

    @Test
    void extractUserIdFromToken_ok() {
        String token = "abc.def.ghi";
        when(jwtUtils.extractUsername(token)).thenReturn("alice");
        User u = new User(); u.setId(42L); u.setUsername("alice"); u.setRole(UserRole.CUSTOMER);
        when(userRepository.findByUsername("alice")).thenReturn(u);

        Long id = userService.extractUserIdFromToken(token);
        assertThat(id).isEqualTo(42L);
    }

    @Test
    void isAdmin_true_si_role_ADMIN() {
        when(request.getHeader("Authorization")).thenReturn("Bearer xyz");
        when(jwtUtils.extractUsername("xyz")).thenReturn("bob");
        User u = new User(); u.setId(1L); u.setUsername("bob"); u.setRole(UserRole.ADMIN);
        when(userRepository.findByUsername("bob")).thenReturn(u);

        assertThat(userService.isAdmin(request)).isTrue();
    }

    @Test
    void getProfile_retourne_dto_quand_existe() {
        User u = new User(); u.setId(7L); u.setUsername("joe");
        when(userRepository.findById(7L)).thenReturn(Optional.of(u));
        UserDto dto = userService.getProfile(7L);
        assertThat(dto).isNotNull();
        assertThat(dto.username()).isEqualTo("joe");
    }
}
