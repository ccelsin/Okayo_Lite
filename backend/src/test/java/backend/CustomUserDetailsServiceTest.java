package backend;


import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collection;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import backend.constants.UserRole;
import backend.models.User;
import backend.repositories.UserRepository;
import backend.services.user.CustomUserDetailsService;

/**
 * Tests unitaires de {@link CustomUserDetailsService}.
 *
 * Objectifs :
 *  - Retour d'un {@link UserDetails} correct quand l'utilisateur existe
 *  - Lève une {@link UsernameNotFoundException} quand l'utilisateur n'existe pas
 *  - Les autorités (rôles) exposées correspondent au rôle de l'entité {@link User}
 *
 * Remarque : on mock uniquement {@link UserRepository}; aucun contexte Spring n'est démarré.
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    @DisplayName("loadUserByUsername : doit retourner un UserDetails avec username, password et rôle mappés")
    void loadUserByUsername_returnsUserDetails_whenUserExists() {
        // Arrange
        User entity = new User();
        entity.setId(42L);
        entity.setUsername("alice");
        entity.setPassword("{bcrypt}hash");
        entity.setRole(UserRole.ADMIN);

        when(userRepository.findByUsername("alice")).thenReturn(entity);

        // Act
        UserDetails details = service.loadUserByUsername("alice");

        // Assert
        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getPassword()).isEqualTo("{bcrypt}hash");

        // Vérifie qu'une seule autorité est présente et qu'elle correspond au rôle ADMIN
        Collection<? extends GrantedAuthority> authorities = details.getAuthorities();
        assertThat(authorities)
            .hasSize(1)
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly(UserRole.ADMIN.name());

        // Vérifie l'appel au repository
        verify(userRepository, times(1)).findByUsername("alice");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("loadUserByUsername : doit lever UsernameNotFoundException quand l'utilisateur n'existe pas")
    void loadUserByUsername_throws_whenUserNotFound() {
        // Arrange
        when(userRepository.findByUsername("ghost")).thenReturn(null);

        // Act
        ThrowingCallable call = () -> service.loadUserByUsername("ghost");

        // Assert
        assertThatThrownBy(call)
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("ghost");

        verify(userRepository, times(1)).findByUsername("ghost");
        verifyNoMoreInteractions(userRepository);
    }
}

