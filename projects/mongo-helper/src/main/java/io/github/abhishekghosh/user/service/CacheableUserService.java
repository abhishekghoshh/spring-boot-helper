package io.github.abhishekghosh.user.service;

import io.github.abhishekghosh.core.exception.ApiException;
import io.github.abhishekghosh.user.dto.UserDTO;
import io.github.abhishekghosh.user.model.User;
import io.github.abhishekghosh.user.repository.CacheableUserRepository;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CacheableUserService implements UserServiceI {

    CacheableUserRepository repository;

    @Cacheable(value = "users", key = "#id")
    public UserDTO getUser(String id) throws ApiException {
        User user = repository.findById(id)
                .orElseThrow(() -> new ApiException("User not found with " + id, HttpStatus.NOT_FOUND.value()));
        return toUserDTO(user);
    }

    @CachePut(value = "users", key = "#result.id")
    public UserDTO createUser(UserDTO userDTO) {
        User user = toUser(userDTO);
        user.setId(null);
        user = repository.save(user);
        return toUserDTO(user);
    }
}
