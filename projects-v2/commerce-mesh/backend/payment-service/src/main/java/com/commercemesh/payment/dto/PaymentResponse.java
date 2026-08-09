package com.commercemesh.payment.dto;

import com.commercemesh.payment.entity.PaymentMethod;
import com.commercemesh.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        String id,
        String userId,
        String orderId,
        String orderNumber,
        BigDecimal amount,
        String currency,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        String transactionId,
        String gatewayResponse,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
}
