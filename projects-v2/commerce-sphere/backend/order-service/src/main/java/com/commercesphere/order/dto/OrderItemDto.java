package com.commercesphere.order.dto;
import java.math.BigDecimal;
public record OrderItemDto(String productId, String productName, BigDecimal price, int quantity) {}
