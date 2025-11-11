package backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import backend.dtos.UserDto;
import backend.models.User;
import backend.services.user.UserMapperService;

/**
 * Tests unitaires de {@link UserMapperService}.
 *
 * On vérifie :
 *  - le mapping entité -> DTO (toutes les propriétés publiques mappées)
 *  - le mapping DTO -> entité
 *  - le mapping d'une liste (taille et ordre préservés)
 *  - le "round-trip" (entité -> DTO -> entité) sur les champs gérés par le mapper
 *  - le cas de liste vide
 *
 * Remarque : le mapper ne gère pas les nulls (appel avec null lèverait un NPE),
 * on ne teste donc pas ce comportement ici.
 */
class UserMapperServiceTest {

    @Test
    @DisplayName("toDto : doit mapper toutes les propriétés visibles depuis User vers UserDto")
    void toDto_should_map_all_fields() {
        // Arrange
        User entity = new User();
        entity.setId(7L);
        entity.setUsername("alice");
        entity.setEmail("alice@example.com");
        entity.setPhoneNumber("0102030405");
        entity.setCodeCustomer("CU1234-5678");
        entity.setAddress("10 rue du Test");
        entity.setPostalCode("75000");
        entity.setCity("Paris");
        entity.setWebsite("https://alice.dev");

        // Act
        UserDto dto = UserMapperService.toDto(entity);

        // Assert
        assertThat(dto.id()).isEqualTo(7L);
        assertThat(dto.username()).isEqualTo("alice");
        assertThat(dto.email()).isEqualTo("alice@example.com");
        assertThat(dto.phoneNumber()).isEqualTo("0102030405");
        assertThat(dto.codeCustomer()).isEqualTo("CU1234-5678");
        assertThat(dto.address()).isEqualTo("10 rue du Test");
        assertThat(dto.postalCode()).isEqualTo("75000");
        assertThat(dto.city()).isEqualTo("Paris");
        assertThat(dto.website()).isEqualTo("https://alice.dev");
    }

    @Test
    @DisplayName("toEntity : doit mapper toutes les propriétés depuis UserDto vers User")
    void toEntity_should_map_all_fields() {
        // Arrange
        UserDto dto = new UserDto(
            9L,
            "bob",
            "bob@example.com",
            "0606060606",
            "CU0001-0002",
            "20 avenue des Tests",
            "13000",
            "Marseille",
            "https://bob.dev"
        );

        // Act
        User entity = UserMapperService.toEntity(dto);

        // Assert (uniquement les champs gérés par le mapper)
        assertThat(entity.getId()).isEqualTo(9L);
        assertThat(entity.getUsername()).isEqualTo("bob");
        assertThat(entity.getEmail()).isEqualTo("bob@example.com");
        assertThat(entity.getPhoneNumber()).isEqualTo("0606060606");
        assertThat(entity.getCodeCustomer()).isEqualTo("CU0001-0002");
        assertThat(entity.getAddress()).isEqualTo("20 avenue des Tests");
        assertThat(entity.getPostalCode()).isEqualTo("13000");
        assertThat(entity.getCity()).isEqualTo("Marseille");
        assertThat(entity.getWebsite()).isEqualTo("https://bob.dev");

        // Les autres champs de User (password, role, relations, etc.) ne sont pas mappés ici.
        assertThat(entity.getPassword()).isNull();
        assertThat(entity.getRole()).isNull();
    }

    @Test
    @DisplayName("toDtoList : doit convertir une liste et préserver taille + ordre")
    void toDtoList_should_map_list_and_preserve_order() {
        // Arrange
        User u1 = new User(); u1.setId(1L); u1.setUsername("user1");
        User u2 = new User(); u2.setId(2L); u2.setUsername("user2");
        List<User> users = List.of(u1, u2);

        // Act
        List<UserDto> dtos = UserMapperService.toDtoList(users);

        // Assert
        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).id()).isEqualTo(1L);
        assertThat(dtos.get(0).username()).isEqualTo("user1");
        assertThat(dtos.get(1).id()).isEqualTo(2L);
        assertThat(dtos.get(1).username()).isEqualTo("user2");
    }

    @Test
    @DisplayName("toDtoList : doit retourner une liste vide si l'entrée est vide")
    void toDtoList_on_empty_input_returns_empty_list() {
        // Arrange
        List<User> empty = new ArrayList<>();

        // Act
        List<UserDto> dtos = UserMapperService.toDtoList(empty);

        // Assert
        assertThat(dtos).isEmpty();
    }

    @Test
    @DisplayName("Round-trip : User -> DTO -> User conserve les champs mappés")
    void round_trip_entity_toDto_toEntity_should_preserve_fields() {
        // Arrange
        User original = new User();
        original.setId(100L);
        original.setUsername("carol");
        original.setEmail("carol@example.com");
        original.setPhoneNumber("0707070707");
        original.setCodeCustomer("CU9999-0000");
        original.setAddress("1 place du Mapper");
        original.setPostalCode("31000");
        original.setCity("Toulouse");
        original.setWebsite("https://carol.dev");

        // Act
        UserDto dto = UserMapperService.toDto(original);
        User mappedBack = UserMapperService.toEntity(dto);

        // Assert : le "round-trip" préserve les champs du mapper
        assertThat(mappedBack.getId()).isEqualTo(original.getId());
        assertThat(mappedBack.getUsername()).isEqualTo(original.getUsername());
        assertThat(mappedBack.getEmail()).isEqualTo(original.getEmail());
        assertThat(mappedBack.getPhoneNumber()).isEqualTo(original.getPhoneNumber());
        assertThat(mappedBack.getCodeCustomer()).isEqualTo(original.getCodeCustomer());
        assertThat(mappedBack.getAddress()).isEqualTo(original.getAddress());
        assertThat(mappedBack.getPostalCode()).isEqualTo(original.getPostalCode());
        assertThat(mappedBack.getCity()).isEqualTo(original.getCity());
        assertThat(mappedBack.getWebsite()).isEqualTo(original.getWebsite());
    }
}
