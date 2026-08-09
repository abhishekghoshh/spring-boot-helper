package com.commercemesh.payment.controller;

import com.commercemesh.payment.dto.PageResponse;
import com.commercemesh.payment.dto.PaymentResponse;
import com.commercemesh.payment.dto.ProcessPaymentRequest;
import com.commercemesh.payment.dto.RefundRequest;
import com.commercemesh.payment.dto.RefundResponse;
import com.commercemesh.payment.service.PaymentService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Process a new payment.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        PaymentResponse response = paymentService.processPayment(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get payment history for the authenticated user (paginated).
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<PaymentResponse>> getPaymentHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = authentication.getName();
        PageResponse<PaymentResponse> response = paymentService.getPaymentHistory(userId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Get payment by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable String id) {
        PaymentResponse response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get payment by order ID.
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(@PathVariable String orderId) {
        PaymentResponse response = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Process a refund for a payment.
     */
    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RefundResponse> processRefund(
            @PathVariable String paymentId,
            @Valid @RequestBody RefundRequest request) {
        RefundResponse response = paymentService.processRefund(paymentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all refunds for a payment.
     */
    @GetMapping("/{paymentId}/refunds")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RefundResponse>> getRefundsForPayment(@PathVariable String paymentId) {
        List<RefundResponse> response = paymentService.getRefundsForPayment(paymentId);
        return ResponseEntity.ok(response);
    }
}
