package io.github.abhishekghosh.user.service;

import io.github.abhishekghosh.core.exception.ApiException;
import io.github.abhishekghosh.user.dto.UserDTO;
import io.github.abhishekghosh.user.model.User;
import io.github.abhishekghosh.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class UserService implements UserServiceI {

    private UserRepository userRepository;

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toUserDTO)
                .toList();
    }

    public UserDTO getUserById(String id) throws ApiException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException("User not found with " + id, HttpStatus.NOT_FOUND.value()));
        return toUserDTO(user);
    }

    public UserDTO createUser(UserDTO userDTO) {
        User user = toUser(userDTO);
        user.setId(null); // Ensure ID is null for a new user
        userRepository.save(user);
        return toUserDTO(user);
    }

    public void deleteUser(String id) throws ApiException {
        if (!userRepository.existsById(id)) {
            throw new ApiException("User not found with " + id, HttpStatus.NOT_FOUND.value());
        }
        userRepository.deleteById(id);
    }

    public UserDTO updateUser(String id, UserDTO userDTO) throws ApiException {
        if (!userRepository.existsById(id)) {
            throw new ApiException("User not found with " + id, HttpStatus.NOT_FOUND.value());
        }
        User user = toUser(userDTO);
        user.setId(id);
        userRepository.save(user);
        return toUserDTO(user);
    }

}