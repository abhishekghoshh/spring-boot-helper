package com.commercemesh.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtTokenProvider")
class JwtTokenProviderTest {

    // A 256-bit key encoded in Base64 for HS256
    private static final String SECRET = "my-very-long-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256-algorithm-12345";
    private static final long ACCESS_TOKEN_EXPIRATION = 3600000L; // 1 hour
    private static final long REFRESH_TOKEN_EXPIRATION = 86400000L; // 24 hours

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION);
    }

    @Nested
    @DisplayName("generateAccessToken")
    class GenerateAccessTokenTests {

        @Test
        @DisplayName("should generate a valid non-null access token")
        void generatesNonNullToken() {
            String token = jwtTokenProvider.generateAccessToken(1L, "testuser", Set.of("CUSTOMER"));

            assertThat(token).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("should encode username as subject in access token")
        void encodesUsernameAsSubject() {
            String token = jwtTokenProvider.generateAccessToken(42L, "john_doe", Set.of("ADMIN"));

            String username = jwtTokenProvider.getUsername(token);
            assertThat(username).isEqualTo("john_doe");
        }

        @Test
        @DisplayName("should encode userId in access token claims")
        void encodesUserIdInClaims() {
            String token = jwtTokenProvider.generateAccessToken(99L, "testuser", Set.of("CUSTOMER"));

            Long userId = jwtTokenProvider.getUserId(token);
            assertThat(userId).isEqualTo(99L);
        }
    }

    @Nested
    @DisplayName("generateRefreshToken")
    class GenerateRefreshTokenTests {

        @Test
        @DisplayName("should generate a valid non-null refresh token")
        void generatesNonNullRefreshToken() {
            String token = jwtTokenProvider.generateRefreshToken(1L);

            assertThat(token).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("should generate different tokens for different users")
        void generatesDifferentTokensForDifferentUsers() {
            String token1 = jwtTokenProvider.generateRefreshToken(1L);
            String token2 = jwtTokenProvider.generateRefreshToken(2L);

            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValidTests {

        @Test
        @DisplayName("should return true for a valid access token")
        void returnsTrueForValidAccessToken() {
            String token = jwtTokenProvider.generateAccessToken(1L, "testuser", Set.of("CUSTOMER"));

            assertThat(jwtTokenProvider.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("should return true for a valid refresh token")
        void returnsTrueForValidRefreshToken() {
            String token = jwtTokenProvider.generateRefreshToken(1L);

            assertThat(jwtTokenProvider.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("should return false for an invalid token")
        void returnsFalseForInvalidToken() {
            assertThat(jwtTokenProvider.isTokenValid("invalid-token-string")).isFalse();
        }

        @Test
        @DisplayName("should return false for an empty token")
        void returnsFalseForEmptyToken() {
            assertThat(jwtTokenProvider.isTokenValid("")).isFalse();
        }

        @Test
        @DisplayName("should return false for a tampered token")
        void returnsFalseForTamperedToken() {
            String token = jwtTokenProvider.generateAccessToken(1L, "testuser", Set.of("CUSTOMER"));
            String tamperedToken = token.substring(0, token.length() - 5) + "xxxxx";

            assertThat(jwtTokenProvider.isTokenValid(tamperedToken)).isFalse();
        }

        @Test
        @DisplayName("should return false for null token")
        void returnsFalseForNullToken() {
            assertThat(jwtTokenProvider.isTokenValid(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("token uniqueness")
    class TokenUniquenessTests {

        @Test
        @DisplayName("should generate unique access tokens on each call")
        void generatesUniqueAccessTokens() {
            String token1 = jwtTokenProvider.generateAccessToken(1L, "testuser", Set.of("CUSTOMER"));
            String token2 = jwtTokenProvider.generateAccessToken(1L, "testuser", Set.of("CUSTOMER"));

            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("should generate unique refresh tokens on each call")
        void generatesUniqueRefreshTokens() {
            String token1 = jwtTokenProvider.generateRefreshToken(1L);
            String token2 = jwtTokenProvider.generateRefreshToken(1L);

            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("cross-provider validation")
    class CrossProviderTests {

        @Test
        @DisplayName("should reject token signed with different secret")
        void rejectsTokenFromDifferentProvider() {
            JwtTokenProvider otherProvider = new JwtTokenProvider(
                    "a-completely-different-secret-key-that-is-also-256-bits-long-for-hs256-algorithm!!",
                    ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION);
            String token = otherProvider.generateAccessToken(1L, "testuser", Set.of("CUSTOMER"));

            assertThat(jwtTokenProvider.isTokenValid(token)).isFalse();
        }
    }
}
