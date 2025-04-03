package io.github.abhishekghosh.jenkinshelper.controller;

import io.github.abhishekghosh.jenkinshelper.model.User;
import io.github.abhishekghosh.jenkinshelper.model.UserException;
import io.github.abhishekghosh.jenkinshelper.service.UserService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {

    private UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @GetMapping
    public List<User> getAllUsers() throws InterruptedException {
        logger.info("Thread info : {} ", Thread.currentThread());
        Thread.sleep(1000);
        List<User> users = userService.getAllUsers();
        logger.info("Thread info : {} ", Thread.currentThread());
        return users;
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable String id) throws UserException {
        return userService.getUserById(id);
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
    }
}