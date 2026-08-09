package com.commercemesh.notification.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderCancelledEvent(
        String orderId,
        String userId,
        String orderNumber,
        BigDecimal totalAmount,
        String customerEmail,
        String customerName,
        String cancellationReason,
        LocalDateTime cancelledAt
) {}
