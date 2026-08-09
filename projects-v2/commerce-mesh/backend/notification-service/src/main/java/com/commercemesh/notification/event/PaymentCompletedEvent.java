package com.commercemesh.notification.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentCompletedEvent(
        String orderId,
        String userId,
        String orderNumber,
        String paymentId,
        BigDecimal amount,
        String customerEmail,
        String customerName,
        String paymentMethod,
        LocalDateTime paidAt
) {}
