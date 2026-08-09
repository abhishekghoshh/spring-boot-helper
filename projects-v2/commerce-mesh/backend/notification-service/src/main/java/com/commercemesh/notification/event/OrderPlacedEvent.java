package com.commercemesh.notification.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderPlacedEvent(
        String orderId,
        String userId,
        String orderNumber,
        BigDecimal totalAmount,
        String customerEmail,
        String customerName,
        LocalDateTime orderDate
) {}
