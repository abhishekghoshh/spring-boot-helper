package com.commercesphere.identity;

import com.commercesphere.identity.dto.LoginRequest;
import com.commercesphere.identity.dto.RegisterRequest;
import com.commercesphere.identity.dto.TokenResponse;
import com.commercesphere.identity.entity.User;
import com.commercesphere.identity.exception.InvalidCredentialsException;
import com.commercesphere.identity.exception.UserAlreadyExistsException;
import com.commercesphere.identity.repository.UserRepository;
import com.commercesphere.identity.service.AuthService;
import com.commercesphere.identity.service.JwtService;
import com.commercesphere.identity.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService(
                "commerce-sphere-jwt-secret-key-that-is-at-least-256-bits-long-for-hs256",
                3600000L, 2592000000L);
        authService = new AuthService(userRepository, passwordEncoder, jwtService, refreshTokenService);
    }

    @Test
    void register_shouldCreateUserAndReturnToken() {
        var request = new RegisterRequest("testuser", "test@example.com",
                "password123", "Test", "User");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("user-123");
            return u;
        });
        when(refreshTokenService.createRefreshToken(anyString(), anyString())).thenReturn("rt-123");

        TokenResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("testuser", response.username());
        assertEquals("Bearer", response.tokenType());
        assertNotNull(response.accessToken());
    }

    @Test
    void register_duplicateUsername_shouldThrow() {
        var request = new RegisterRequest("existing", "new@example.com",
                "password123", "Test", "User");
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
    }

    @Test
    void login_validCredentials_shouldReturnToken() {
        var request = new LoginRequest("testuser", "password123");
        User user = User.builder()
                .id("user-123").username("testuser")
                .email("test@example.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("Test").lastName("User")
                .enabled(true).roles(Set.of("ROLE_USER"))
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(refreshTokenService.createRefreshToken(anyString(), anyString())).thenReturn("rt-123");

        TokenResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("testuser", response.username());
    }

    @Test
    void login_invalidPassword_shouldThrow() {
        var request = new LoginRequest("testuser", "wrongpassword");
        User user = User.builder()
                .id("user-123").username("testuser")
                .password(passwordEncoder.encode("password123"))
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }
}
