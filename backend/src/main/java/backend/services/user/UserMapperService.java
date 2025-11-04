package backend.services.user;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import backend.dtos.UserDto;
import backend.models.User;

@Service
public class UserMapperService {

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

    public static List<UserDto> toDtoList(List<User> user) {
        List<UserDto> customerDtos = new ArrayList<>();
        for (User customer : user) {
            customerDtos.add(toDto(customer));
        }
        return customerDtos;
    }
    
}

