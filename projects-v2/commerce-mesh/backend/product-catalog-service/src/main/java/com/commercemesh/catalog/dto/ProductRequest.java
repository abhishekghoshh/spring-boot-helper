package com.commercemesh.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductRequest(
        @NotBlank(message = "Product name is required")
        String name,

        @NotBlank(message = "Product description is required")
        String description,

        String brand,

        @NotBlank(message = "SKU is required")
        String sku,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        BigDecimal price,

        BigDecimal compareAtPrice,

        String currency,

        @NotBlank(message = "Category ID is required")
        String categoryId,

        List<String> images,

        List<String> tags,

        Map<String, String> attributes,

        boolean isActive,

        boolean isFeatured
) {
}
