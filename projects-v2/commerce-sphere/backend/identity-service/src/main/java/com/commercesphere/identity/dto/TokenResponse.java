package com.commercesphere.identity.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String username,
        String email,
        String fullName
) {
    public TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn,
                         String username, String email, String fullName) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
    }
}
