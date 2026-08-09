package com.commercemesh.payment.dto;

import com.commercemesh.payment.entity.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record InvoiceResponse(
        String id,
        String orderId,
        String orderNumber,
        String userId,
        BigDecimal amount,
        String currency,
        InvoiceStatus status,
        String items,
        Instant createdAt,
        Instant generatedAt
) {
}
