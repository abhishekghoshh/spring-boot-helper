package com.loansphere.auth.dto;

public record UserResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        java.util.Set<String> roles,
        boolean emailVerified) {}
