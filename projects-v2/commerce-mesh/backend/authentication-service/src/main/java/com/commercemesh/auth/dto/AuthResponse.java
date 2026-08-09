package com.commercemesh.auth.dto;

import java.util.Set;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String username,
        Set<String> roles
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, String username, Set<String> roles) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, username, roles);
    }
}
