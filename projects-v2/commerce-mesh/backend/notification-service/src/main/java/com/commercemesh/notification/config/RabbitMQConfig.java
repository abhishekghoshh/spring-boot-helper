package com.commercemesh.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String ORDER_NOTIFICATIONS_QUEUE = "order.notifications";
    public static final String ORDER_NOTIFICATIONS_DLQ = "order.notifications.dlq";

    public static final String ORDER_EVENTS_EXCHANGE = "order.events";
    public static final String ORDER_EVENTS_DLX = "order.events.dlx";

    public static final String ORDER_ROUTING_PREFIX = "order.*";
    public static final String PAYMENT_ROUTING_PREFIX = "payment.*";

    // -- Dead Letter Exchange --
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(ORDER_EVENTS_DLX);
    }

    // -- Dead Letter Queue --
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(ORDER_NOTIFICATIONS_DLQ).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(ORDER_NOTIFICATIONS_DLQ);
    }

    // -- Main Queue (bound to DLX) --
    @Bean
    public Queue orderNotificationsQueue() {
        return QueueBuilder.durable(ORDER_NOTIFICATIONS_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_EVENTS_DLX)
                .withArgument("x-dead-letter-routing-key", ORDER_NOTIFICATIONS_DLQ)
                .build();
    }

    // -- Topic Exchange --
    @Bean
    public TopicExchange orderEventsExchange() {
        return new TopicExchange(ORDER_EVENTS_EXCHANGE);
    }

    // -- Bindings --
    @Bean
    public Binding orderNotificationsBinding() {
        return BindingBuilder.bind(orderNotificationsQueue())
                .to(orderEventsExchange())
                .with(ORDER_ROUTING_PREFIX);
    }

    @Bean
    public Binding paymentNotificationsBinding() {
        return BindingBuilder.bind(orderNotificationsQueue())
                .to(orderEventsExchange())
                .with(PAYMENT_ROUTING_PREFIX);
    }

    // -- Message Converter (JSON) --
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
