package com.commercemesh.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AddToCartRequest(
        @NotBlank String productId,
        @NotBlank String name,
        @NotNull BigDecimal price,
        @Min(1) int quantity,
        String image
) {}
