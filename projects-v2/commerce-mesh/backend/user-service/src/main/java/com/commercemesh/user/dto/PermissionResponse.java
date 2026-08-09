package com.commercemesh.user.dto;

import com.commercemesh.user.entity.Permission;

public record PermissionResponse(
        Long id,
        String name,
        String description
) {
        public static PermissionResponse fromEntity(Permission permission) {
                return new PermissionResponse(permission.getId(), permission.getName(), permission.getDescription());
        }
}
