package io.github.abhishekghosh.user.controller;

import io.github.abhishekghosh.core.exception.ApiException;
import io.github.abhishekghosh.user.dto.UserDTO;
import io.github.abhishekghosh.user.service.CacheableUserService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/users")
@AllArgsConstructor
public class CacheableUserController {
    private static final Logger logger = LoggerFactory.getLogger(CacheableUserController.class);

    private final CacheableUserService userService;

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public UserDTO getUser(@PathVariable String id) throws ApiException {
        return userService.getUser(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDTO createUser(@RequestBody UserDTO userDTO) {
        return userService.createUser(userDTO);
    }
}
