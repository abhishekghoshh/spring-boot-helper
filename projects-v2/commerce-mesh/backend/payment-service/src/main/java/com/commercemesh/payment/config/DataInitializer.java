package com.commercemesh.payment.config;

import com.commercemesh.payment.service.PaymentGatewaySimulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Application data initializer.
 * <p>
 * For the payment-service, no sample data is created at startup.
 * The DataInitializer exists as a configuration anchor for future use
 * (e.g. seeding test gateways, health check hooks, etc.).
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    public DataInitializer(PaymentGatewaySimulator gatewaySimulator) {
        log.info("Payment service initialized. PaymentGatewaySimulator is ready.");
    }
}
