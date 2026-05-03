package com.ecommerce.stock_service.service;

import com.ecommerce.stock_service.dto.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockSagaHandler {

    private final StockDomainService stock;
    private final RabbitTemplate rabbit;

    @Value("${stock.rabbit.exchange}") private String exchange;
    @Value("${stock.rabbit.reservedRoutingKey}") private String reservedKey;
    @Value("${stock.rabbit.rejectedRoutingKey}") private String rejectedKey;

    @Transactional
    @RabbitListener(queues = "${stock.rabbit.reserveRequestedQueue}")
    public void handleReserveRequested(StockEventPayloads.StockReserveRequestedEvent event) {
        log.info("[SAGA] StockReserveRequested alındı. orderId={}", event.getOrderId());

        StockUpdateRequest req = new StockUpdateRequest(
                event.getItems().stream()
                        .map(i -> new StockUpdateRequest.StockItem(i.getProductId(), i.getQuantity()))
                        .collect(Collectors.toList())
        );

        StockUpdateResponse resp = stock.reserve(req);

        if (resp.isSuccess()) {
            rabbit.convertAndSend(exchange, reservedKey,
                    new StockEventPayloads.StockReservedEvent(event.getOrderId(), event.getUsername(), "Stok rezerve edildi"));
            log.info("[SAGA] StockReserved gönderildi. orderId={}", event.getOrderId());
        } else {
            rabbit.convertAndSend(exchange, rejectedKey,
                    new StockEventPayloads.StockRejectedEvent(event.getOrderId(), event.getUsername(), resp.getMessage()));
            log.warn("[SAGA] StockRejected gönderildi. orderId={}, reason={}", event.getOrderId(), resp.getMessage());
        }
    }
}