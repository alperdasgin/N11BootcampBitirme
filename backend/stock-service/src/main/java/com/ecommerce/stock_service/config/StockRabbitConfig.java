package com.ecommerce.stock_service.config;

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
public class StockRabbitConfig {

    @Value("${stock.rabbit.exchange}") private String exchangeName;
    @Value("${stock.rabbit.reserveRequestedQueue}") private String reserveRequestedQueue;
    @Value("${stock.rabbit.reserveRequestedRoutingKey}") private String reserveRequestedRoutingKey;

    @Value("${product.sync.exchange}") private String productSyncExchange;
    @Value("${product.sync.queue}") private String productSyncQueue;
    @Value("${product.sync.routingKeyPattern}") private String productSyncRoutingKeyPattern;

    @Bean
    public TopicExchange stockExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue reserveRequestedQueue() {
        return QueueBuilder.durable(reserveRequestedQueue).build();
    }

    @Bean
    public Binding reserveRequestedBinding() {
        return BindingBuilder.bind(reserveRequestedQueue())
                .to(stockExchange())
                .with(reserveRequestedRoutingKey);
    }

    // ── Product → Stock sync events ──────────────────────────────
    @Bean
    public TopicExchange productSyncExchange() {
        return new TopicExchange(productSyncExchange, true, false);
    }

    @Bean
    public Queue productSyncQueue() {
        return QueueBuilder.durable(productSyncQueue).build();
    }

    @Bean
    public Binding productSyncBinding() {
        return BindingBuilder.bind(productSyncQueue())
                .to(productSyncExchange())
                .with(productSyncRoutingKeyPattern);
    }

    @Bean
    public MessageConverter jsonConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(jsonConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory cf) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(cf);
        factory.setMessageConverter(jsonConverter());
        return factory;
    }
}