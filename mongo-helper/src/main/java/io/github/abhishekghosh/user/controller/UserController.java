package io.github.abhishekghosh.user.controller;

import io.github.abhishekghosh.core.dto.SuccessDTO;
import io.github.abhishekghosh.core.exception.ApiException;
import io.github.abhishekghosh.user.dto.UserDTO;
import io.github.abhishekghosh.user.service.UserService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class UserController {

    private UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<UserDTO> getAllUsers() throws InterruptedException {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public UserDTO getUserById(@PathVariable String id) throws ApiException {
        return userService.getUserById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDTO createUser(@RequestBody UserDTO userDTO) {
        return userService.createUser(userDTO);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public UserDTO createUser(@PathVariable("id") String id, @RequestBody UserDTO userDTO) throws ApiException {
        return userService.updateUser(id, userDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<SuccessDTO> deleteUser(@PathVariable String id) throws ApiException {
        userService.deleteUser(id);
        return ResponseEntity.accepted()
                .body(new SuccessDTO("Successfully deleted"));
    }
}