package com.commercemesh.payment.service;

import com.commercemesh.payment.dto.PaymentResponse;
import com.commercemesh.payment.dto.ProcessPaymentRequest;
import com.commercemesh.payment.dto.RefundRequest;
import com.commercemesh.payment.dto.RefundResponse;
import com.commercemesh.payment.dto.PageResponse;
import com.commercemesh.payment.entity.Invoice;
import com.commercemesh.payment.entity.InvoiceStatus;
import com.commercemesh.payment.entity.Payment;
import com.commercemesh.payment.entity.PaymentStatus;
import com.commercemesh.payment.entity.Refund;
import com.commercemesh.payment.entity.RefundStatus;
import com.commercemesh.payment.exception.PaymentProcessingException;
import com.commercemesh.payment.exception.RefundProcessingException;
import com.commercemesh.payment.exception.ResourceNotFoundException;
import com.commercemesh.payment.repository.InvoiceRepository;
import com.commercemesh.payment.repository.PaymentRepository;
import com.commercemesh.payment.repository.RefundRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentGatewaySimulator paymentGatewaySimulator;

    public PaymentService(PaymentRepository paymentRepository,
                          RefundRepository refundRepository,
                          InvoiceRepository invoiceRepository,
                          PaymentGatewaySimulator paymentGatewaySimulator) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentGatewaySimulator = paymentGatewaySimulator;
    }

    /**
     * Process a payment. The actual gateway call goes through PaymentGatewaySimulator
     * which has its own @CircuitBreaker and @Retry annotations.
     * The service layer @Retry acts as an additional safety net.
     */
    @Retry(name = "paymentRetry")
    @CircuitBreaker(name = "paymentCB")
    @Transactional
    public PaymentResponse processPayment(ProcessPaymentRequest request, String userId) {
        log.info("Processing payment for order {} by user {}", request.orderId(), userId);

        String currency = request.currency() != null && !request.currency().isBlank()
                ? request.currency().toUpperCase() : "USD";

        // Create and save the payment in PENDING state
        Payment payment = new Payment(
                userId,
                request.orderId(),
                request.orderNumber(),
                request.amount(),
                currency,
                request.paymentMethod()
        );
        payment.setStatus(PaymentStatus.PROCESSING);
        payment = paymentRepository.save(payment);

        try {
            // Call the simulated gateway (has its own circuit breaker + retry)
            PaymentGatewaySimulator.GatewayResponse gatewayResponse =
                    paymentGatewaySimulator.processPayment(payment);

            if (gatewayResponse.success()) {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTransactionId(gatewayResponse.transactionId());
                payment.setGatewayResponse(gatewayResponse.rawResponse());
                payment = paymentRepository.save(payment);

                // Generate invoice on successful payment
                generateInvoiceForPayment(payment);

                log.info("Payment {} completed successfully for order {}",
                        payment.getId(), request.orderId());
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setGatewayResponse(gatewayResponse.rawResponse());
                payment.setErrorMessage(gatewayResponse.errorMessage());
                payment = paymentRepository.save(payment);
                log.warn("Payment {} failed for order {}: {}",
                        payment.getId(), request.orderId(), gatewayResponse.errorMessage());
            }
        } catch (Exception e) {
            log.error("Payment processing threw exception for payment {}: {}", payment.getId(), e.getMessage());
            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorMessage(e.getMessage());
            payment = paymentRepository.save(payment);
            throw new PaymentProcessingException("Payment processing failed: " + e.getMessage(), e);
        }

        return mapToPaymentResponse(payment);
    }

    /**
     * Fallback for processPayment when the circuit breaker is open or retries exhausted.
     */
    public PaymentResponse processPaymentFallback(ProcessPaymentRequest request, String userId, Throwable t) {
        log.error("Circuit breaker / retry fallback triggered for processPayment. Order: {}, Reason: {}",
                request.orderId(), t.getMessage());

        // Save a FAILED payment record
        Payment payment = new Payment(
                userId,
                request.orderId(),
                request.orderNumber(),
                request.amount(),
                request.currency() != null ? request.currency() : "USD",
                request.paymentMethod()
        );
        payment.setStatus(PaymentStatus.FAILED);
        payment.setErrorMessage("Payment service temporarily unavailable. Reason: " + t.getMessage());
        payment = paymentRepository.save(payment);

        return mapToPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderId));
        return mapToPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(String id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
        return mapToPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> getPaymentHistory(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Payment> paymentPage = paymentRepository.findByUserId(userId, pageable);

        List<PaymentResponse> responses = paymentPage.getContent().stream()
                .map(this::mapToPaymentResponse)
                .toList();

        return new PageResponse<>(
                responses,
                paymentPage.getNumber(),
                paymentPage.getSize(),
                paymentPage.getTotalElements(),
                paymentPage.getTotalPages()
        );
    }

    /**
     * Process a refund for a completed payment.
     */
    @CircuitBreaker(name = "paymentCB")
    @Transactional
    public RefundResponse processRefund(String paymentId, RefundRequest request) {
        log.info("Processing refund for payment {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new RefundProcessingException(
                    "Payment " + paymentId + " is not in COMPLETED state (current: " + payment.getStatus() + ")");
        }

        if (request.amount().compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException(
                    "Refund amount " + request.amount() + " exceeds payment amount " + payment.getAmount());
        }

        // Create refund record
        Refund refund = new Refund(paymentId, payment.getOrderId(), request.amount(), request.reason());
        refund.setStatus(RefundStatus.PROCESSING);
        refund = refundRepository.save(refund);

        try {
            // Call the simulated gateway for refund
            PaymentGatewaySimulator.GatewayResponse gatewayResponse =
                    paymentGatewaySimulator.processRefund(payment, refund);

            if (gatewayResponse.success()) {
                refund.setStatus(RefundStatus.COMPLETED);
                refund.setTransactionId(gatewayResponse.transactionId());
                refund = refundRepository.save(refund);

                // Update payment and invoice status
                boolean isFullRefund = request.amount().compareTo(payment.getAmount()) == 0;
                payment.setStatus(isFullRefund ? PaymentStatus.REFUNDED : PaymentStatus.COMPLETED);
                paymentRepository.save(payment);

                updateInvoiceForRefund(payment, isFullRefund);

                log.info("Refund {} completed for payment {}", refund.getId(), paymentId);
            } else {
                refund.setStatus(RefundStatus.FAILED);
                refund = refundRepository.save(refund);
                log.warn("Refund {} failed for payment {}: {}",
                        refund.getId(), paymentId, gatewayResponse.errorMessage());
            }
        } catch (Exception e) {
            log.error("Refund processing threw exception for refund {}: {}", refund.getId(), e.getMessage());
            refund.setStatus(RefundStatus.FAILED);
            refundRepository.save(refund);
            throw new RefundProcessingException("Refund processing failed: " + e.getMessage(), e);
        }

        return mapToRefundResponse(refund);
    }

    /**
     * Fallback for processRefund when the circuit breaker is open.
     */
    public RefundResponse processRefundFallback(String paymentId, RefundRequest request, Throwable t) {
        log.error("Circuit breaker fallback triggered for refund. Payment: {}, Reason: {}",
                paymentId, t.getMessage());

        Refund refund = new Refund(paymentId, "", request.amount(), request.reason());
        refund.setStatus(RefundStatus.FAILED);
        refund = refundRepository.save(refund);

        return mapToRefundResponse(refund);
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> getRefundsForPayment(String paymentId) {
        List<Refund> refunds = refundRepository.findByPaymentId(paymentId);
        return refunds.stream()
                .map(this::mapToRefundResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RefundResponse getRefundStatus(String refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found: " + refundId));
        return mapToRefundResponse(refund);
    }

    // --- Invoice helpers ---

    private void generateInvoiceForPayment(Payment payment) {
        Invoice invoice = new Invoice(
                payment.getOrderId(),
                payment.getOrderNumber(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                InvoiceStatus.PAID,
                "[]" // items JSON will be populated later if order-service provides
        );
        invoiceRepository.save(invoice);
        log.info("Invoice generated for order {} payment {}", payment.getOrderId(), payment.getId());
    }

    private void updateInvoiceForRefund(Payment payment, boolean isFullRefund) {
        invoiceRepository.findByOrderId(payment.getOrderId()).ifPresent(invoice -> {
            if (isFullRefund) {
                invoice.setStatus(InvoiceStatus.REFUNDED);
            } else {
                invoice.setStatus(InvoiceStatus.PARTIALLY_REFUNDED);
            }
            invoiceRepository.save(invoice);
        });
    }

    // --- Mapping helpers ---

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getUserId(),
                payment.getOrderId(),
                payment.getOrderNumber(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getTransactionId(),
                payment.getGatewayResponse(),
                payment.getErrorMessage(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    private RefundResponse mapToRefundResponse(Refund refund) {
        return new RefundResponse(
                refund.getId(),
                refund.getPaymentId(),
                refund.getOrderId(),
                refund.getAmount(),
                refund.getReason(),
                refund.getStatus(),
                refund.getTransactionId(),
                refund.getCreatedAt()
        );
    }
}
