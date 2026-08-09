package com.loansphere.auth.dto;

public record TokenResponse(String accessToken, String refreshToken, String tokenType) {
    public TokenResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, "Bearer");
    }
}
