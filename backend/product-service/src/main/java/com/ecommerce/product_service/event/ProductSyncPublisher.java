package com.ecommerce.product_service.event;

import com.ecommerce.product_service.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductSyncPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${product.sync.exchange}")
    private String exchange;

    @Value("${product.sync.routingKey.created}")
    private String createdKey;

    @Value("${product.sync.routingKey.updated}")
    private String updatedKey;

    @Value("${product.sync.routingKey.deleted}")
    private String deletedKey;

    public void publishCreated(Product product) {
        send(createdKey, ProductSyncEvent.builder()
                .productId(product.getId())
                .productName(product.getName())
                .stock(product.getStock() != null ? product.getStock() : 0)
                .type(ProductSyncEvent.Type.CREATED)
                .build());
    }

    public void publishUpdated(Product product) {
        send(updatedKey, ProductSyncEvent.builder()
                .productId(product.getId())
                .productName(product.getName())
                .stock(product.getStock() != null ? product.getStock() : 0)
                .type(ProductSyncEvent.Type.UPDATED)
                .build());
    }

    public void publishDeleted(Long productId, String productName) {
        send(deletedKey, ProductSyncEvent.builder()
                .productId(productId)
                .productName(productName)
                .stock(0)
                .type(ProductSyncEvent.Type.DELETED)
                .build());
    }

    private void send(String routingKey, ProductSyncEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("ProductSyncEvent gönderildi: type={}, productId={}, stock={}",
                    event.getType(), event.getProductId(), event.getStock());
        } catch (AmqpException e) {
            log.warn("ProductSyncEvent gönderilemedi (RabbitMQ erişilemiyor olabilir): {}", e.getMessage());
        }
    }
}
