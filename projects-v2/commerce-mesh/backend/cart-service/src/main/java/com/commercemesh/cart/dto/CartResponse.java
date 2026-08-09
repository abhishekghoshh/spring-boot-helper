package com.commercemesh.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        String id,
        String userId,
        List<CartItemResponse> items,
        String couponCode,
        BigDecimal discount,
        BigDecimal subtotal,
        BigDecimal total
) {}
