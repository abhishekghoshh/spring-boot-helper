package com.commercemesh.notification.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderShippedEvent(
        String orderId,
        String userId,
        String orderNumber,
        BigDecimal totalAmount,
        String customerEmail,
        String customerName,
        String trackingNumber,
        String carrier,
        LocalDateTime shippedAt,
        LocalDateTime estimatedDelivery
) {}
