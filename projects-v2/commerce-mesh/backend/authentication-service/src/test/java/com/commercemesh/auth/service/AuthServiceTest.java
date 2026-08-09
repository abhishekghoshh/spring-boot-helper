package com.commercemesh.auth.service;

import com.commercemesh.auth.dto.AuthResponse;
import com.commercemesh.auth.dto.LoginRequest;
import com.commercemesh.auth.dto.RefreshTokenRequest;
import com.commercemesh.auth.dto.RegisterRequest;
import com.commercemesh.auth.entity.RefreshToken;
import com.commercemesh.auth.entity.User;
import com.commercemesh.auth.repository.RefreshTokenRepository;
import com.commercemesh.auth.repository.UserRepository;
import com.commercemesh.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private User user;
    private final String accessToken = "access-token-abc";
    private final String refreshTokenValue = "refresh-token-xyz";
    private final String encodedPassword = "$2a$10$encodedPasswordHash";

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword(encodedPassword);
        user.setFullName("Test User");
        user.setEmailVerified(false);
        user.getRoles().add("CUSTOMER");

        // Common stubs
        lenient().when(jwtTokenProvider.generateAccessToken(eq(1L), eq("testuser"), any()))
                .thenReturn(accessToken);
        lenient().when(jwtTokenProvider.generateRefreshToken(1L)).thenReturn(refreshTokenValue);
        lenient().when(passwordEncoder.encode(anyString())).thenReturn(encodedPassword);
        lenient().when(authenticationManager.authenticate(any())).thenReturn(authentication);
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("should return AuthResponse when credentials are valid")
        void loginWithValidCredentialsReturnsAuthResponse() {
            LoginRequest request = new LoginRequest("testuser", "password123");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            AuthResponse response = authService.login(request);

            assertThat(response.accessToken()).isEqualTo(accessToken);
            assertThat(response.refreshToken()).isEqualTo(refreshTokenValue);
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.username()).isEqualTo("testuser");
            assertThat(response.roles()).contains("CUSTOMER");

            verify(authenticationManager).authenticate(
                    new UsernamePasswordAuthenticationToken("testuser", "password123"));
            verify(userRepository).findByUsername("testuser");
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("should throw BadCredentialsException when password is invalid")
        void loginWithInvalidPasswordThrowsBadCredentialsException() {
            LoginRequest request = new LoginRequest("testuser", "wrongpassword");
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Bad credentials");

            verify(userRepository, never()).findByUsername(anyString());
        }

        @Test
        @DisplayName("should throw BadCredentialsException when user not found after auth")
        void loginWithUserNotFoundAfterAuthThrowsBadCredentialsException() {
            LoginRequest request = new LoginRequest("testuser", "password123");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Invalid credentials");
        }
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("should return AuthResponse when registering a new user")
        void registerNewUserReturnsAuthResponse() {
            RegisterRequest request = new RegisterRequest("newuser", "new@example.com", "password123", "New User");
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

            AuthResponse response = authService.register(request);

            assertThat(response.accessToken()).isEqualTo(accessToken);
            assertThat(response.refreshToken()).isEqualTo(refreshTokenValue);
            assertThat(response.username()).isEqualTo("newuser");
            assertThat(response.roles()).contains("CUSTOMER");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getUsername()).isEqualTo("newuser");
            assertThat(savedUser.getEmail()).isEqualTo("new@example.com");
            assertThat(savedUser.getPassword()).isEqualTo(encodedPassword);
            assertThat(savedUser.getFullName()).isEqualTo("New User");
            assertThat(savedUser.isEmailVerified()).isFalse();
            assertThat(savedUser.getVerificationToken()).isNotNull();
            assertThat(savedUser.getRoles()).contains("CUSTOMER");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when username already exists")
        void registerWithDuplicateUsernameThrowsException() {
            RegisterRequest request = new RegisterRequest("testuser", "new@example.com", "password123", "Test User");
            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Username already taken");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when email already exists")
        void registerWithDuplicateEmailThrowsException() {
            RegisterRequest request = new RegisterRequest("newuser", "test@example.com", "password123", "Test User");
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email already registered");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTests {

        @Test
        @DisplayName("should return new tokens when refresh token is valid")
        void refreshTokenWithValidTokenReturnsNewTokens() {
            RefreshTokenRequest request = new RefreshTokenRequest(refreshTokenValue);
            RefreshToken storedToken = new RefreshToken();
            storedToken.setId(1L);
            storedToken.setToken(refreshTokenValue);
            storedToken.setUserId(1L);
            storedToken.setExpiry(Instant.now().plusSeconds(3600));
            storedToken.setRevoked(false);

            when(refreshTokenRepository.findByToken(refreshTokenValue))
                    .thenReturn(Optional.of(storedToken));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            AuthResponse response = authService.refreshToken(request);

            assertThat(response.accessToken()).isEqualTo(accessToken);
            assertThat(response.refreshToken()).isEqualTo(refreshTokenValue);
            assertThat(response.username()).isEqualTo("testuser");

            verify(refreshTokenRepository).save(storedToken);
            assertThat(storedToken.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when refresh token is revoked")
        void refreshTokenWithRevokedTokenThrowsException() {
            RefreshTokenRequest request = new RefreshTokenRequest(refreshTokenValue);
            RefreshToken storedToken = new RefreshToken();
            storedToken.setId(1L);
            storedToken.setToken(refreshTokenValue);
            storedToken.setUserId(1L);
            storedToken.setExpiry(Instant.now().plusSeconds(3600));
            storedToken.setRevoked(true);

            when(refreshTokenRepository.findByToken(refreshTokenValue))
                    .thenReturn(Optional.of(storedToken));

            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("revoked");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when refresh token is expired")
        void refreshTokenWithExpiredTokenThrowsException() {
            RefreshTokenRequest request = new RefreshTokenRequest(refreshTokenValue);
            RefreshToken storedToken = new RefreshToken();
            storedToken.setId(1L);
            storedToken.setToken(refreshTokenValue);
            storedToken.setUserId(1L);
            storedToken.setExpiry(Instant.now().minusSeconds(3600));
            storedToken.setRevoked(false);

            when(refreshTokenRepository.findByToken(refreshTokenValue))
                    .thenReturn(Optional.of(storedToken));

            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when token not found")
        void refreshTokenWithUnknownTokenThrowsException() {
            RefreshTokenRequest request = new RefreshTokenRequest("unknown-token");
            when(refreshTokenRepository.findByToken("unknown-token"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid refresh token");
        }
    }

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPasswordTests {

        @Test
        @DisplayName("should create reset token for existing email")
        void forgotPasswordCreatesResetToken() {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

            authService.forgotPassword("test@example.com");

            verify(userRepository).save(user);
            assertThat(user.getResetToken()).isNotNull();
            assertThat(user.getResetTokenExpiry()).isNotNull();
            assertThat(user.getResetTokenExpiry()).isAfter(Instant.now());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when email not found")
        void forgotPasswordWithUnknownEmailThrowsException() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.forgotPassword("unknown@example.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email not found");
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPasswordTests {

        @Test
        @DisplayName("should update password with valid reset token")
        void resetPasswordWithValidTokenUpdatesPassword() {
            String resetToken = "valid-reset-token";
            user.setResetToken(resetToken);
            user.setResetTokenExpiry(Instant.now().plusSeconds(3600));
            when(userRepository.findByResetToken(resetToken)).thenReturn(Optional.of(user));

            authService.resetPassword(resetToken, "newSecurePass123");

            verify(passwordEncoder).encode("newSecurePass123");
            verify(userRepository).save(user);
            assertThat(user.getPassword()).isEqualTo(encodedPassword);
            assertThat(user.getResetToken()).isNull();
            assertThat(user.getResetTokenExpiry()).isNull();
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when reset token is expired")
        void resetPasswordWithExpiredTokenThrowsException() {
            String resetToken = "expired-reset-token";
            user.setResetToken(resetToken);
            user.setResetTokenExpiry(Instant.now().minusSeconds(3600));
            when(userRepository.findByResetToken(resetToken)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.resetPassword(resetToken, "newSecurePass123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Reset token expired");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when reset token is invalid")
        void resetPasswordWithInvalidTokenThrowsException() {
            when(userRepository.findByResetToken("invalid-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword("invalid-token", "newSecurePass123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid reset token");
        }
    }
}
