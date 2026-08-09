package com.commercemesh.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "payment-service")
public interface PaymentClient {

    @PostMapping("/api/v1/payments/process")
    Map<String, Object> processPayment(@RequestBody PaymentRequest request);

    record PaymentRequest(Long orderId, String orderNumber, java.math.BigDecimal amount, String paymentMethod) {}
}
