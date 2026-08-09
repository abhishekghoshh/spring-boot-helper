package com.commercemesh.payment.dto;

import com.commercemesh.payment.entity.RefundStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record RefundResponse(
        String id,
        String paymentId,
        String orderId,
        BigDecimal amount,
        String reason,
        RefundStatus status,
        String transactionId,
        Instant createdAt
) {
}
