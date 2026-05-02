package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.*;
import com.ecommerce.order_service.entity.*;
import com.ecommerce.order_service.repository.OrderRepository;
import com.ecommerce.order_service.saga.PaymentCardStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;
    private final PaymentCardStore paymentCardStore;

    @Value("${stock.rabbit.exchange}") private String stockExchange;
    @Value("${stock.rabbit.reserveRequestedRoutingKey}") private String stockReserveKey;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        // 1) Sipariş oluştur
        Order order = Order.builder()
                .username(request.getUsername())
                .status(OrderStatus.CREATED)
                .totalPrice(request.getItems().stream()
                        .mapToDouble(i -> i.getPrice() * i.getQuantity()).sum())
                .build();

        List<OrderItem> items = request.getItems().stream().map(dto ->
                OrderItem.builder()
                        .productId(dto.getProductId())
                        .productName(dto.getProductName())
                        .price(dto.getPrice())
                        .quantity(dto.getQuantity())
                        .order(order)
                        .build()).collect(Collectors.toList());
        order.setItems(items);

        OrderDetails details = OrderDetails.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .streetAddress(request.getStreetAddress())
                .city(request.getCity())
                .country(request.getCountry())
                .phone(request.getPhone())
                .email(request.getEmail())
                .order(order)
                .build();
        order.setOrderDetails(details);

        Order saved = orderRepository.save(order);
        log.info("Sipariş oluşturuldu. orderId={}", saved.getId());

        // 2) Kart bilgisini RAM'e kaydet
        if (request.getCard() != null) {
            PaymentCardStore.CardInfo card = new PaymentCardStore.CardInfo();
            card.setCardHolderName(request.getCard().getCardHolderName());
            card.setCardNumber(request.getCard().getCardNumber());
            card.setExpireMonth(request.getCard().getExpireMonth());
            card.setExpireYear(request.getCard().getExpireYear());
            card.setCvc(request.getCard().getCvc());
            paymentCardStore.put(saved.getId(), card);
        }

        // 3) Saga başlat — stok rezervasyon eventi yayınla
        StockReserveRequestedEvent event = new StockReserveRequestedEvent(
                saved.getId(), saved.getUsername(),
                saved.getItems().stream()
                        .map(i -> new StockReserveRequestedEvent.Item(i.getProductId(), i.getQuantity()))
                        .collect(Collectors.toList())
        );
        rabbitTemplate.convertAndSend(stockExchange, stockReserveKey, event);
        log.info("StockReserveRequested yayınlandı. orderId={}", saved.getId());

        return toResponse(saved);
    }

    public List<OrderResponse> findAllOrders() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    public OrderResponse getOrderById(Long id) {
        return toResponse(orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sipariş bulunamadı: " + id)));
    }

    public List<OrderResponse> findOrdersByUsername(String username) {
        return orderRepository.findByUsername(username).stream().map(this::toResponse).toList();
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .username(order.getUsername())
                .status(order.getStatus().name())
                .totalPrice(order.getTotalPrice())
                .items(order.getItems().stream().map(i ->
                        OrderResponse.OrderItemResponse.builder()
                                .productId(i.getProductId())
                                .productName(i.getProductName())
                                .price(i.getPrice())
                                .quantity(i.getQuantity())
                                .build()).toList())
                .build();
    }
}