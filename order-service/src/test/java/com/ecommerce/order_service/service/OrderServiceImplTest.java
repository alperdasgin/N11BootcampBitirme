package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.CreateOrderRequest;
import com.ecommerce.order_service.dto.OrderResponse;
import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.entity.OrderDetails;
import com.ecommerce.order_service.entity.OrderItem;
import com.ecommerce.order_service.entity.OrderStatus;
import com.ecommerce.order_service.repository.OrderRepository;
import com.ecommerce.order_service.saga.PaymentCardStore;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl Unit Testleri")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private PaymentCardStore paymentCardStore;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        // application.properties'deki @Value alanlarını test ortamında elle set ediyoruz
        ReflectionTestUtils.setField(orderService, "stockExchange", "stock.events.exchange");
        ReflectionTestUtils.setField(orderService, "stockReserveKey", "order.stock.reserve.requested");
    }

    // ─────────────────────────────────────────────────────────────
    // createOrder testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Sipariş başarıyla oluşturulur ve CREATED statüsünde kaydedilir")
    void createOrder_shouldSaveOrderWithCreatedStatus() {
        // ARRANGE (Hazırlık): Gelen istek ve kaydedilecek Order nesnesi hazırlanıyor
        CreateOrderRequest request = buildSampleRequest(false);

        Order savedOrder = buildSampleOrder(1L, OrderStatus.CREATED);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // ACT (Çalıştır): Metodu çağır
        OrderResponse response = orderService.createOrder(request);

        // ASSERT (Doğrula): Beklentileri kontrol et
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("CREATED");
        assertThat(response.getUsername()).isEqualTo("alper");

        // Repository'nin save() metodunun çağrıldığını doğrula
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Sipariş oluşturulunca RabbitMQ'ya StockReserveRequested eventi yayınlanır")
    void createOrder_shouldPublishStockReserveRequestedEvent() {
        // ARRANGE
        CreateOrderRequest request = buildSampleRequest(false);
        Order savedOrder = buildSampleOrder(1L, OrderStatus.CREATED);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // ACT
        orderService.createOrder(request);

        // ASSERT: RabbitTemplate'in doğru exchange ve routing key ile çağrıldığını doğrula
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq("stock.events.exchange"),
                eq("order.stock.reserve.requested"),
                any(Object.class)
        );
    }

    @Test
    @DisplayName("Kart bilgisi varsa PaymentCardStore'a kaydedilir")
    void createOrder_shouldStoreCardInfoWhenCardProvided() {
        // ARRANGE
        CreateOrderRequest request = buildSampleRequest(true); // kart bilgisiyle
        Order savedOrder = buildSampleOrder(1L, OrderStatus.CREATED);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // ACT
        orderService.createOrder(request);

        // ASSERT: Kart bilgisinin store'a kaydedildiğini doğrula
        verify(paymentCardStore, times(1)).put(eq(1L), any(PaymentCardStore.CardInfo.class));
    }

    @Test
    @DisplayName("Kart bilgisi yoksa PaymentCardStore'a kayıt yapılmaz")
    void createOrder_shouldNotStoreCardInfoWhenCardIsNull() {
        // ARRANGE
        CreateOrderRequest request = buildSampleRequest(false); // kart yok
        Order savedOrder = buildSampleOrder(1L, OrderStatus.CREATED);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // ACT
        orderService.createOrder(request);

        // ASSERT: PaymentCardStore'un hiç çağrılmadığını doğrula
        verify(paymentCardStore, never()).put(anyLong(), any());
    }

    @Test
    @DisplayName("Toplam fiyat, ürün fiyatları çarpı miktarların toplamı olarak doğru hesaplanır")
    void createOrder_shouldCalculateTotalPriceCorrectly() {
        // ARRANGE: 2 ürün — 100.0 x 2 adet + 50.0 x 1 adet = 250.0
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUsername("alper");

        CreateOrderRequest.OrderItemDto item1 = new CreateOrderRequest.OrderItemDto();
        item1.setProductId(1L);
        item1.setProductName("Laptop");
        item1.setPrice(100.0);
        item1.setQuantity(2);

        CreateOrderRequest.OrderItemDto item2 = new CreateOrderRequest.OrderItemDto();
        item2.setProductId(2L);
        item2.setProductName("Mouse");
        item2.setPrice(50.0);
        item2.setQuantity(1);

        request.setItems(List.of(item1, item2));

        // Kaydedilen Order'ı yakalamak için ArgumentCaptor kullanıyoruz
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        when(orderRepository.save(orderCaptor.capture())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });

        // ACT
        orderService.createOrder(request);

        // ASSERT: Yakalanan Order'ın totalPrice'ını kontrol et
        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getTotalPrice()).isEqualTo(250.0);
    }

    // ─────────────────────────────────────────────────────────────
    // getOrderById testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Var olan sipariş ID'siyle sipariş başarıyla getirilir")
    void getOrderById_shouldReturnOrderWhenExists() {
        // ARRANGE
        Order order = buildSampleOrder(1L, OrderStatus.COMPLETED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // ACT
        OrderResponse response = orderService.getOrderById(1L);

        // ASSERT
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("Var olmayan sipariş ID'siyle hata fırlatılır")
    void getOrderById_shouldThrowExceptionWhenNotFound() {
        // ARRANGE
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        // ASSERT: Exception fırlatıldığını doğrula
        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Sipariş bulunamadı: 99");
    }

    // ─────────────────────────────────────────────────────────────
    // findOrdersByUsername testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Kullanıcı adıyla siparişler başarıyla listelenir")
    void findOrdersByUsername_shouldReturnOrdersForUser() {
        // ARRANGE
        List<Order> orders = List.of(
                buildSampleOrder(1L, OrderStatus.COMPLETED),
                buildSampleOrder(2L, OrderStatus.CANCELLED)
        );
        when(orderRepository.findByUsername("alper")).thenReturn(orders);

        // ACT
        List<OrderResponse> responses = orderService.findOrdersByUsername("alper");

        // ASSERT
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getStatus()).isEqualTo("COMPLETED");
        assertThat(responses.get(1).getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("Siparişi olmayan kullanıcı için boş liste döner")
    void findOrdersByUsername_shouldReturnEmptyListWhenNoOrders() {
        // ARRANGE
        when(orderRepository.findByUsername("yenikullanici")).thenReturn(List.of());

        // ACT
        List<OrderResponse> responses = orderService.findOrdersByUsername("yenikullanici");

        // ASSERT
        assertThat(responses).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────
    // updateOrderStatus testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Sipariş durumu başarıyla güncellenir")
    void updateOrderStatus_shouldUpdateStatusCorrectly() {
        // ARRANGE
        Order order = buildSampleOrder(1L, OrderStatus.CREATED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // ACT
        orderService.updateOrderStatus(1L, "COMPLETED");

        // ASSERT: Siparişin durumunun COMPLETED yapıldığını doğrula
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("Var olmayan sipariş güncellenmeye çalışılınca hata fırlatılır")
    void updateOrderStatus_shouldThrowExceptionWhenOrderNotFound() {
        // ARRANGE
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        // ASSERT
        assertThatThrownBy(() -> orderService.updateOrderStatus(99L, "COMPLETED"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Sipariş bulunamadı: 99");
    }

    // ─────────────────────────────────────────────────────────────
    // Yardımcı metotlar
    // ─────────────────────────────────────────────────────────────

    private CreateOrderRequest buildSampleRequest(boolean withCard) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUsername("alper");
        request.setFirstName("Alper");
        request.setLastName("Daşgın");
        request.setCity("İstanbul");
        request.setCountry("Turkey");
        request.setPhone("05551234567");
        request.setEmail("alper@test.com");
        request.setStreetAddress("Test Sokak No:1");

        CreateOrderRequest.OrderItemDto item = new CreateOrderRequest.OrderItemDto();
        item.setProductId(1L);
        item.setProductName("Wireless Mouse");
        item.setPrice(299.99);
        item.setQuantity(1);
        request.setItems(List.of(item));

        if (withCard) {
            CreateOrderRequest.Card card = new CreateOrderRequest.Card();
            card.setCardHolderName("Alper Daşgın");
            card.setCardNumber("5528790000000008");
            card.setExpireMonth("12");
            card.setExpireYear("2030");
            card.setCvc("123");
            request.setCard(card);
        }

        return request;
    }

    private Order buildSampleOrder(Long id, OrderStatus status) {
        OrderItem item = OrderItem.builder()
                .id(1L)
                .productId(1L)
                .productName("Wireless Mouse")
                .price(299.99)
                .quantity(1)
                .build();

        OrderDetails details = OrderDetails.builder()
                .firstName("Alper")
                .lastName("Daşgın")
                .city("İstanbul")
                .country("Turkey")
                .phone("05551234567")
                .email("alper@test.com")
                .streetAddress("Test Sokak No:1")
                .build();

        return Order.builder()
                .id(id)
                .username("alper")
                .totalPrice(299.99)
                .status(status)
                .items(List.of(item))
                .orderDetails(details)
                .build();
    }
}
