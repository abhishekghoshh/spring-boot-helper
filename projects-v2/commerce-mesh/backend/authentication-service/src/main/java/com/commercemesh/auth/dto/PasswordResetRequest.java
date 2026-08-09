package com.commercemesh.auth.dto;

public record PasswordResetRequest(
        @jakarta.validation.constraints.Email @jakarta.validation.constraints.NotBlank String email
) {}
