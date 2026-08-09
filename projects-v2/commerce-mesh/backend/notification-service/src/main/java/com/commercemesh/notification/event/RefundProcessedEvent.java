package com.commercemesh.notification.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundProcessedEvent(
        String orderId,
        String userId,
        String orderNumber,
        String refundId,
        BigDecimal refundAmount,
        String customerEmail,
        String customerName,
        String refundReason,
        LocalDateTime refundedAt
) {}
