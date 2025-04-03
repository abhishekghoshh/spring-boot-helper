package io.github.abhishekghosh.jenkinshelper.service;

import io.github.abhishekghosh.jenkinshelper.model.User;
import io.github.abhishekghosh.jenkinshelper.model.UserException;
import io.github.abhishekghosh.jenkinshelper.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class UserService {

    private UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(String id) throws UserException {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserException(HttpStatus.NOT_FOUND, "User not found with " + id));
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }
}