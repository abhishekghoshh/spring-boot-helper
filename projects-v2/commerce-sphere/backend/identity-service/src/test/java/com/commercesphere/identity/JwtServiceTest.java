package com.commercesphere.identity;

import com.commercesphere.identity.entity.User;
import com.commercesphere.identity.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "commerce-sphere-jwt-secret-key-that-is-at-least-256-bits-long-for-hs256",
                3600000L, 2592000000L);
    }

    @Test
    void generateAndValidateAccessToken() {
        User user = User.builder()
                .id("user-123").username("testuser")
                .email("test@example.com")
                .firstName("Test").lastName("User")
                .roles(Set.of("ROLE_USER"))
                .build();

        String token = jwtService.generateAccessToken(user);

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
        assertEquals("user-123", jwtService.getUserIdFromToken(token));
        assertEquals("testuser", jwtService.getUsernameFromToken(token));
        assertTrue(jwtService.getRolesFromToken(token).contains("ROLE_USER"));
    }

    @Test
    void invalidToken_shouldReturnNull() {
        assertFalse(jwtService.isTokenValid("invalid-token"));
        assertNull(jwtService.getUserIdFromToken("invalid-token"));
    }
}
