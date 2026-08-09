package com.commercemesh.order.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "order.events";
    public static final String ORDER_NOTIFICATIONS_QUEUE = "order.notifications";

    @Bean
    public TopicExchange orderEventsExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderNotificationsQueue() {
        return QueueBuilder.durable(ORDER_NOTIFICATIONS_QUEUE).build();
    }

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder
                .bind(orderNotificationsQueue())
                .to(orderEventsExchange())
                .with("order.created");
    }

    @Bean
    public Binding orderConfirmedBinding() {
        return BindingBuilder
                .bind(orderNotificationsQueue())
                .to(orderEventsExchange())
                .with("order.confirmed");
    }

    @Bean
    public Binding orderShippedBinding() {
        return BindingBuilder
                .bind(orderNotificationsQueue())
                .to(orderEventsExchange())
                .with("order.shipped");
    }

    @Bean
    public Binding orderDeliveredBinding() {
        return BindingBuilder
                .bind(orderNotificationsQueue())
                .to(orderEventsExchange())
                .with("order.delivered");
    }

    @Bean
    public Binding orderCancelledBinding() {
        return BindingBuilder
                .bind(orderNotificationsQueue())
                .to(orderEventsExchange())
                .with("order.cancelled");
    }

    @Bean
    public Binding orderPaidBinding() {
        return BindingBuilder
                .bind(orderNotificationsQueue())
                .to(orderEventsExchange())
                .with("order.paid");
    }
}
