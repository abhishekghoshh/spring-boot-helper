package com.commercemesh.auth.dto;

public record RefreshTokenRequest(
        @jakarta.validation.constraints.NotBlank String refreshToken
) {}
