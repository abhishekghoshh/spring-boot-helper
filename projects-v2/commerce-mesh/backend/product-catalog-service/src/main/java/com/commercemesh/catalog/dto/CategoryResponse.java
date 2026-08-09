package com.commercemesh.catalog.dto;

import com.commercemesh.catalog.document.Category;

import java.time.LocalDateTime;
import java.util.List;

public record CategoryResponse(
        String id,
        String name,
        String slug,
        String description,
        String image,
        String parentId,
        boolean isActive,
        int sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CategoryResponse> children
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getImage(),
                category.getParentId(),
                category.isActive(),
                category.getSortOrder(),
                category.getCreatedAt(),
                category.getUpdatedAt(),
                null
        );
    }

    public static CategoryResponse from(Category category, List<CategoryResponse> children) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getImage(),
                category.getParentId(),
                category.isActive(),
                category.getSortOrder(),
                category.getCreatedAt(),
                category.getUpdatedAt(),
                children
        );
    }
}
