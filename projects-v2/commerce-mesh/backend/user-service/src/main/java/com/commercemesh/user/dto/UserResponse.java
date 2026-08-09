package com.commercemesh.user.dto;

import java.time.Instant;
import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String phone,
        String avatarUrl,
        String preferences,
        Set<RoleResponse> roles,
        Instant createdAt
) {}
