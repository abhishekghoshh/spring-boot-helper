package com.commercemesh.order.dto;

import java.time.Instant;

public record OrderEventResponse(
        Long id,
        Long orderId,
        String eventType,
        String payload,
        Instant createdAt
) {}
