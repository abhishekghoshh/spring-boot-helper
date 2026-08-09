package com.commercemesh.cart.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        String productId,
        String name,
        BigDecimal price,
        int quantity,
        String image
) {}
