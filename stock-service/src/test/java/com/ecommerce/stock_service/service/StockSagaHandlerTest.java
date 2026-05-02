package com.ecommerce.stock_service.service;

import com.ecommerce.stock_service.dto.StockEventPayloads;
import com.ecommerce.stock_service.dto.StockUpdateRequest;
import com.ecommerce.stock_service.dto.StockUpdateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockSagaHandler Unit Testleri")
class StockSagaHandlerTest {

    @Mock
    private StockDomainService stockService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private StockSagaHandler sagaHandler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sagaHandler, "exchange", "stock.events.exchange");
        ReflectionTestUtils.setField(sagaHandler, "reservedKey", "order.stock.reserved");
        ReflectionTestUtils.setField(sagaHandler, "rejectedKey", "order.stock.rejected");
    }

    @Test
    @DisplayName("Rezervasyon başarılıysa StockReservedEvent gönderilir")
    void handleReserveRequested_whenReservationSucceeds_shouldSendReservedEvent() {
        // ARRANGE
        StockEventPayloads.StockReserveRequestedEvent event = new StockEventPayloads.StockReserveRequestedEvent(
                1L, "alper", List.of(new StockEventPayloads.StockReserveRequestedEvent.Item(10L, 2))
        );

        when(stockService.reserve(any(StockUpdateRequest.class)))
                .thenReturn(StockUpdateResponse.ok("Stok rezerve edildi"));

        // ACT
        sagaHandler.handleReserveRequested(event);

        // ASSERT: RabbitTemplate doğru exchange ve routing key ile çağrılmalı
        ArgumentCaptor<Object> messageCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq("stock.events.exchange"),
                eq("order.stock.reserved"),
                messageCaptor.capture()
        );

        // Gönderilen mesajın içeriğini doğrula
        Object sentMessage = messageCaptor.getValue();
        assertThat(sentMessage).isInstanceOf(StockEventPayloads.StockReservedEvent.class);
        StockEventPayloads.StockReservedEvent reservedEvent = (StockEventPayloads.StockReservedEvent) sentMessage;
        assertThat(reservedEvent.getOrderId()).isEqualTo(1L);
        assertThat(reservedEvent.getUsername()).isEqualTo("alper");
        assertThat(reservedEvent.getMessage()).isEqualTo("Stok rezerve edildi");
    }

    @Test
    @DisplayName("Rezervasyon başarısızsa StockRejectedEvent gönderilir")
    void handleReserveRequested_whenReservationFails_shouldSendRejectedEvent() {
        // ARRANGE
        StockEventPayloads.StockReserveRequestedEvent event = new StockEventPayloads.StockReserveRequestedEvent(
                2L, "ahmet", List.of(new StockEventPayloads.StockReserveRequestedEvent.Item(20L, 5))
        );

        when(stockService.reserve(any(StockUpdateRequest.class)))
                .thenReturn(StockUpdateResponse.fail("Yetersiz stok"));

        // ACT
        sagaHandler.handleReserveRequested(event);

        // ASSERT: RabbitTemplate rejected key ile çağrılmalı
        ArgumentCaptor<Object> messageCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq("stock.events.exchange"),
                eq("order.stock.rejected"),
                messageCaptor.capture()
        );

        // Gönderilen mesajın içeriğini doğrula
        Object sentMessage = messageCaptor.getValue();
        assertThat(sentMessage).isInstanceOf(StockEventPayloads.StockRejectedEvent.class);
        StockEventPayloads.StockRejectedEvent rejectedEvent = (StockEventPayloads.StockRejectedEvent) sentMessage;
        assertThat(rejectedEvent.getOrderId()).isEqualTo(2L);
        assertThat(rejectedEvent.getUsername()).isEqualTo("ahmet");
        assertThat(rejectedEvent.getReason()).isEqualTo("Yetersiz stok");
    }

    @Test
    @DisplayName("Gelen Event nesnesi StockUpdateRequest'e doğru dönüştürülür")
    void handleReserveRequested_shouldMapEventToRequestCorrectly() {
        // ARRANGE
        StockEventPayloads.StockReserveRequestedEvent event = new StockEventPayloads.StockReserveRequestedEvent(
                1L, "alper", List.of(
                        new StockEventPayloads.StockReserveRequestedEvent.Item(10L, 2),
                        new StockEventPayloads.StockReserveRequestedEvent.Item(20L, 1)
                )
        );

        // Request'i yakalamak için ArgumentCaptor
        ArgumentCaptor<StockUpdateRequest> requestCaptor = ArgumentCaptor.forClass(StockUpdateRequest.class);
        when(stockService.reserve(requestCaptor.capture()))
                .thenReturn(StockUpdateResponse.ok("Başarılı"));

        // ACT
        sagaHandler.handleReserveRequested(event);

        // ASSERT: İstek doğru dönüştürülmüş mü?
        StockUpdateRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getItems()).hasSize(2);
        
        assertThat(capturedRequest.getItems().get(0).getProductId()).isEqualTo(10L);
        assertThat(capturedRequest.getItems().get(0).getQuantity()).isEqualTo(2);
        
        assertThat(capturedRequest.getItems().get(1).getProductId()).isEqualTo(20L);
        assertThat(capturedRequest.getItems().get(1).getQuantity()).isEqualTo(1);
    }
}
