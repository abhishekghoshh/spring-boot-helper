package com.commercemesh.cart.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record WishlistItemResponse(
        String productId,
        String name,
        BigDecimal price,
        String image,
        Instant addedAt
) {}
