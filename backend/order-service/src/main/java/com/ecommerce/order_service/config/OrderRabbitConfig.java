package com.ecommerce.order_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class OrderRabbitConfig {

    @Value("${stock.rabbit.exchange}") private String stockExchange;
    @Value("${stock.rabbit.reservedRoutingKey}") private String reservedKey;
    @Value("${stock.rabbit.rejectedRoutingKey}") private String rejectedKey;
    @Value("${order.rabbit.stockReservedQueue}") private String reservedQueue;
    @Value("${order.rabbit.stockRejectedQueue}") private String rejectedQueue;

    @Bean
    public TopicExchange stockEventsExchange() {
        return new TopicExchange(stockExchange, true, false);
    }

    @Bean
    public Queue orderStockReservedQueue() {
        return QueueBuilder.durable(reservedQueue).build();
    }

    @Bean
    public Queue orderStockRejectedQueue() {
        return QueueBuilder.durable(rejectedQueue).build();
    }

    @Bean
    public Binding reservedBinding() {
        return BindingBuilder.bind(orderStockReservedQueue()).to(stockEventsExchange()).with(reservedKey);
    }

    @Bean
    public Binding rejectedBinding() {
        return BindingBuilder.bind(orderStockRejectedQueue()).to(stockEventsExchange()).with(rejectedKey);
    }

    @Bean
    public MessageConverter jsonConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(jsonConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory cf) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(cf);
        factory.setMessageConverter(jsonConverter());
        return factory;
    }
}