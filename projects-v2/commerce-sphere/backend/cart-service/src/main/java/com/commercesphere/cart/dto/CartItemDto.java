package com.commercesphere.cart.dto;
import java.math.BigDecimal;
public record CartItemDto(String productId, String productName, BigDecimal price, int quantity, String imageUrl) {}
