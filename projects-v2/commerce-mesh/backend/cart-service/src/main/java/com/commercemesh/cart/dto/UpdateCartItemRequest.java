package com.commercemesh.cart.dto;

import jakarta.validation.constraints.Min;

public record UpdateCartItemRequest(
        @Min(0) int quantity    // 0 means remove the item
) {}
