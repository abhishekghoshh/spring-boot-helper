package com.commercesphere.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public record BrandDto(
        String id, @NotBlank String name, String description, String logoUrl
) {}
