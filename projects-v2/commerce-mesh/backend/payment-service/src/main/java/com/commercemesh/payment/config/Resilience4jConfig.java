package com.commercemesh.payment.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Explicit Resilience4j configuration as a programmatic fallback
 * if application.yml properties are not picked up.
 * <p>
 * The customizer beans override defaults only when the standard
 * resilience4j.* properties are absent or disabled.
 */
@Configuration
public class Resilience4jConfig {

    private static final Logger log = LoggerFactory.getLogger(Resilience4jConfig.class);

    private static final String PAYMENT_CB = "paymentCB";
    private static final String PAYMENT_RETRY = "paymentRetry";

    /**
     * Programmatic CircuitBreaker configuration for the "paymentCB" instance.
     * These values apply as defaults; application.yml config takes precedence.
     */
    @Bean
    @ConditionalOnProperty(name = "resilience4j.circuitbreaker.configs.default.register-health-indicator",
            havingValue = "true", matchIfMissing = true)
    public CircuitBreakerConfig paymentCircuitBreakerConfig() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        log.info("CircuitBreaker config initialized: failureRate=50%, waitInOpen=10s, windowSize=10");
        return config;
    }

    /**
     * Registers the custom CircuitBreaker config with the default Registry
     * so the "paymentCB" name is recognized even without YAML config.
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerConfig paymentCircuitBreakerConfig) {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        registry.addConfiguration(PAYMENT_CB, paymentCircuitBreakerConfig);
        log.info("CircuitBreaker '{}' registered in CircuitBreakerRegistry", PAYMENT_CB);
        return registry;
    }

    /**
     * Programmatic Retry configuration for the "paymentRetry" instance.
     */
    @Bean
    public RetryConfig paymentRetryConfig() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .retryExceptions(Exception.class)
                .build();

        log.info("Retry config initialized: maxAttempts=3, waitDuration=1s");
        return config;
    }

    /**
     * Registers the custom Retry config with the default Registry.
     */
    @Bean
    public RetryRegistry retryRegistry(RetryConfig paymentRetryConfig) {
        RetryRegistry registry = RetryRegistry.ofDefaults();
        registry.addConfiguration(PAYMENT_RETRY, paymentRetryConfig);
        log.info("Retry '{}' registered in RetryRegistry", PAYMENT_RETRY);
        return registry;
    }
}
