package backend.services.user;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import backend.dtos.UserDto;
import backend.models.User;

/**
 * Service utilitaire de mappage entre les entités {@link User}
 * et leurs représentations de transfert de données {@link UserDto}.
 * 
 * <p>Ce service centralise la logique de conversion des objets afin d’éviter
 * la duplication du code de transformation dans les autres couches de l’application.</p>
 *
 * <p>Annoté avec {@link Service} pour permettre son injection dans d’autres
 * composants Spring si nécessaire.</p>
 */
@Service
public class UserMapperService {

    /**
     * Convertit une entité {@link User} en un objet {@link UserDto}.
     *
     * @param user l'entité utilisateur à convertir
     * @return un {@link UserDto} contenant les informations publiques de l’utilisateur
     */
    public static UserDto toDto(User user){
        return new UserDto(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getCodeCustomer(),
            user.getAddress(),
            user.getPostalCode(),
            user.getCity(),
            user.getWebsite()
        );
    }

    /**
     * Convertit un objet {@link UserDto} en une entité {@link User}.
     *
     * @param user le DTO utilisateur à convertir
     * @return une nouvelle instance de {@link User} remplie à partir du DTO
     */
    public static User toEntity(UserDto user){
        User entity = new User();
        entity.setId(user.id());
        entity.setUsername(user.username());
        entity.setEmail(user.email());
        entity.setPhoneNumber(user.phoneNumber());
        entity.setCodeCustomer(user.codeCustomer());
        entity.setAddress(user.address());
        entity.setPostalCode(user.postalCode());
        entity.setCity(user.city());
        entity.setWebsite(user.website());
        return entity;
    }

    /**
     * Convertit une liste d’entités {@link User} en une liste de {@link UserDto}.
     *
     * @param user liste d’entités utilisateurs
     * @return liste de {@link UserDto} correspondante
     */
    public static List<UserDto> toDtoList(List<User> user) {
        List<UserDto> customerDtos = new ArrayList<>();
        for (User customer : user) {
            customerDtos.add(toDto(customer));
        }
        return customerDtos;
    }
}
