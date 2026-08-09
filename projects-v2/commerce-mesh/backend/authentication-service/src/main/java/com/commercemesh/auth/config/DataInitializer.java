package com.commercemesh.auth.config;

import jakarta.annotation.PostConstruct;
import com.commercemesh.auth.entity.User;
import com.commercemesh.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void init() {
        if (userRepository.existsByUsername("admin")) {
            return;
        }

        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@commercemesh.com");
        admin.setPassword(passwordEncoder.encode("Admin@123"));
        admin.setFullName("System Administrator");
        admin.setEmailVerified(true);
        admin.setRoles(Set.of("SUPER_ADMIN", "ADMIN"));
        userRepository.save(admin);
    }
}
