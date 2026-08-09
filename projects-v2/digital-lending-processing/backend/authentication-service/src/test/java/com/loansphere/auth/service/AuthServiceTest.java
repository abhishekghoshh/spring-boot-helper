package com.loansphere.auth.service;

import com.loansphere.auth.model.User;
import com.loansphere.auth.repository.UserRepository;
import com.loansphere.common.exception.BadRequestException;
import com.loansphere.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtSecret", "test-secret-key-that-is-at-least-256-bits-long-for-testing");
        ReflectionTestUtils.setField(authService, "jwtExpirationMs", 3600000L);
        ReflectionTestUtils.setField(authService, "refreshExpirationMs", 86400000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        var request = buildUser("testuser", "test@test.com");
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(request);

        User result = authService.register("testuser", "test@test.com", "password", "First", "Last");

        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getRoles()).contains("CUSTOMER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowWhenUsernameExists() {
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("taken", "email@test.com", "pw", "F", "L"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Username already taken");
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenEmailExists() {
        when(userRepository.existsByUsername("user")).thenReturn(false);
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("user", "taken@test.com", "pw", "F", "L"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void shouldFindByUsername() {
        User user = buildUser("test", "test@test.com");
        when(userRepository.findByUsername("test")).thenReturn(Optional.of(user));

        User result = authService.findByUsername("test");

        assertThat(result.getUsername()).isEqualTo("test");
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.findByUsername("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldGenerateAccessToken() {
        User user = buildUser("test", "test@test.com");
        user.setId("abc123");
        user.setRoles(Set.of("CUSTOMER"));

        String token = authService.generateAccessToken(user);

        assertThat(token).isNotNull().startsWith("eyJ");
    }

    @Test
    void shouldGenerateRefreshToken() {
        User user = buildUser("test", "test@test.com");
        user.setId("abc123");

        String token = authService.generateRefreshToken(user);

        assertThat(token).isNotNull();
        verify(valueOps).set(eq("refresh:" + token), eq("abc123"), any(Duration.class));
    }

    @Test
    void shouldBlacklistToken() {
        authService.blacklistToken("token123", 3600000L);

        verify(valueOps).set(eq("blacklist:token123"), eq("revoked"), any(Duration.class));
    }

    @Test
    void shouldDetectBlacklistedToken() {
        when(redisTemplate.hasKey("blacklist:revoked-token")).thenReturn(true);

        assertThat(authService.isTokenBlacklisted("revoked-token")).isTrue();
    }

    private User buildUser(String username, String email) {
        User user = new User();
        user.setId("id-" + username);
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName("First");
        user.setLastName("Last");
        user.setPassword("encoded");
        user.setRoles(Set.of("CUSTOMER"));
        return user;
    }
}
