package com.commercesphere.cart.dto;
import java.math.BigDecimal;
import java.util.List;
public record CartDto(String id, List<CartItemDto> items, BigDecimal totalAmount, int totalItems) {}
