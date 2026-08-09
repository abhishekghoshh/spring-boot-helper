package com.commercesphere.cart.dto;
import java.math.BigDecimal;
public record WishlistItemDto(String productId, String productName, BigDecimal price, String imageUrl) {}
