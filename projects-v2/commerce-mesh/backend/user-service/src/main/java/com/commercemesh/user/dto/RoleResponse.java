package com.commercemesh.user.dto;

import com.commercemesh.user.entity.Role;

import java.util.Set;
import java.util.stream.Collectors;

public record RoleResponse(
        Long id,
        String name,
        String description,
        Set<PermissionResponse> permissions
) {
        public static RoleResponse fromEntity(Role role) {
                Set<PermissionResponse> perms = role.getPermissions() != null
                        ? role.getPermissions().stream()
                                .map(PermissionResponse::fromEntity)
                                .collect(Collectors.toSet())
                        : Set.of();
                return new RoleResponse(role.getId(), role.getName(), role.getDescription(), perms);
        }
}
