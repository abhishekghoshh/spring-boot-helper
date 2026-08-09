package com.commercesphere.order.dto;
import java.math.BigDecimal; import java.time.Instant; import java.util.List;
public record OrderDto(String id, String orderNumber, String userId, String userEmail, List<OrderItemDto> items, BigDecimal subtotal, BigDecimal tax, BigDecimal shipping, BigDecimal total, String status, String shippingAddress, Instant createdAt) {}
