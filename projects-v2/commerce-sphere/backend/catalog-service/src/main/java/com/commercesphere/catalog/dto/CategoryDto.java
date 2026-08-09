package com.commercesphere.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryDto(
        String id, @NotBlank String name, @NotBlank String slug,
        String description, String imageUrl
) {}
