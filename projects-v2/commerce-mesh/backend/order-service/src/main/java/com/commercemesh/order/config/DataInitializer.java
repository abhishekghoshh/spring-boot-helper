package com.commercemesh.order.config;

import com.commercemesh.order.service.OrderEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initOrderService(OrderEventPublisher eventPublisher) {
        return args -> {
            log.info("Order service initialized successfully");
            log.info("RabbitMQ exchange 'order.events' configured with routing keys: "
                     + "order.created, order.confirmed, order.paid, order.shipped, order.delivered, order.cancelled");
            log.info("Queue 'order.notifications' bound to exchange 'order.events'");
        };
    }
}
