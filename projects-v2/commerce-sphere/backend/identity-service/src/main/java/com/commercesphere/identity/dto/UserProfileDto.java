package com.commercesphere.identity.dto;

public record UserProfileDto(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        java.util.Set<String> roles
) {}
