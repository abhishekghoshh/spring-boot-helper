package com.commercesphere.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotBlank String sku, @NotBlank String name, @NotBlank String description,
        @NotNull @Positive BigDecimal price, String currency,
        @NotBlank String categoryId, @NotBlank String brandId,
        boolean active, boolean featured
) {}
