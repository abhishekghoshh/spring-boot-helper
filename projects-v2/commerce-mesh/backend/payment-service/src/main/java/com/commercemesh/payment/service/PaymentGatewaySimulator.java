package com.commercemesh.payment.service;

import com.commercemesh.payment.entity.Payment;
import com.commercemesh.payment.entity.PaymentStatus;
import com.commercemesh.payment.entity.Refund;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Simulates an external payment gateway API.
 * 90% success rate to demonstrate circuit breaker opening when failures accumulate.
 * Includes a 3-second simulated delay to mimic real-world latency.
 */
@Service
public class PaymentGatewaySimulator {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewaySimulator.class);

    /**
     * Gateway response record — internal to the simulator.
     */
    public record GatewayResponse(
            boolean success,
            String transactionId,
            String rawResponse,
            String errorMessage
    ) {
        public static GatewayResponse success(String transactionId, String rawResponse) {
            return new GatewayResponse(true, transactionId, rawResponse, null);
        }

        public static GatewayResponse failure(String rawResponse, String errorMessage) {
            return new GatewayResponse(false, null, rawResponse, errorMessage);
        }
    }

    /**
     * Simulates processing a payment through the external gateway.
     * Uses @CircuitBreaker and @Retry. The circuit breaker name "paymentCB"
     * must match what is configured in application.yml and Resilience4jConfig.
     */
    @CircuitBreaker(name = "paymentCB")
    @Retry(name = "paymentRetry")
    public GatewayResponse processPayment(Payment payment) {
        simulateNetworkDelay();

        // 90% success rate — random failures to demonstrate circuit breaker
        if (Math.random() < 0.90) {
            String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String rawResponse = """
                    {
                      "gateway": "SimulatedGateway",
                      "status": "APPROVED",
                      "transactionId": "%s",
                      "amount": %s,
                      "currency": "%s"
                    }""".formatted(transactionId, payment.getAmount(), payment.getCurrency());

            log.info("Gateway: Payment {} processed successfully. TxID: {}", payment.getId(), transactionId);
            return GatewayResponse.success(transactionId, rawResponse);
        } else {
            String rawResponse = """
                    {
                      "gateway": "SimulatedGateway",
                      "status": "DECLINED",
                      "errorCode": "GATEWAY_DECLINED",
                      "message": "Card declined by issuing bank"
                    }""";
            log.warn("Gateway: Payment {} declined by simulated gateway", payment.getId());
            return GatewayResponse.failure(rawResponse, "Card declined by issuing bank");
        }
    }

    /**
     * Fallback method for processPayment when circuit breaker is open or retries are exhausted.
     */
    public GatewayResponse processPaymentFallback(Payment payment, Throwable t) {
        log.error("Payment gateway circuit breaker fallback triggered for payment {}. Reason: {}",
                payment.getId(), t.getMessage());
        return GatewayResponse.failure(
                "{\"gateway\":\"SimulatedGateway\",\"status\":\"FALLBACK\"}",
                "Payment gateway unavailable. Reason: " + t.getMessage()
        );
    }

    /**
     * Simulates processing a refund through the external gateway.
     */
    @CircuitBreaker(name = "paymentCB")
    @Retry(name = "paymentRetry")
    public GatewayResponse processRefund(Payment payment, Refund refund) {
        simulateNetworkDelay();

        // 90% success rate for refunds too
        if (Math.random() < 0.90) {
            String transactionId = "RFN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String rawResponse = """
                    {
                      "gateway": "SimulatedGateway",
                      "status": "REFUND_APPROVED",
                      "transactionId": "%s",
                      "originalTransactionId": "%s",
                      "refundAmount": %s
                    }""".formatted(transactionId,
                    payment.getTransactionId() != null ? payment.getTransactionId() : "N/A",
                    refund.getAmount());

            log.info("Gateway: Refund {} processed successfully for payment {}. TxID: {}",
                    refund.getId(), payment.getId(), transactionId);
            return GatewayResponse.success(transactionId, rawResponse);
        } else {
            String rawResponse = """
                    {
                      "gateway": "SimulatedGateway",
                      "status": "REFUND_DECLINED",
                      "errorCode": "REFUND_GATEWAY_DECLINED",
                      "message": "Refund declined by gateway"
                    }""";
            log.warn("Gateway: Refund {} declined for payment {}", refund.getId(), payment.getId());
            return GatewayResponse.failure(rawResponse, "Refund declined by gateway");
        }
    }

    /**
     * Fallback method for processRefund when circuit breaker is open or retries are exhausted.
     */
    public GatewayResponse processRefundFallback(Payment payment, Refund refund, Throwable t) {
        log.error("Refund gateway circuit breaker fallback triggered for refund {}. Reason: {}",
                refund.getId(), t.getMessage());
        return GatewayResponse.failure(
                "{\"gateway\":\"SimulatedGateway\",\"status\":\"FALLBACK\"}",
                "Refund gateway unavailable. Reason: " + t.getMessage()
        );
    }

    private void simulateNetworkDelay() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Gateway call interrupted", e);
        }
    }
}
