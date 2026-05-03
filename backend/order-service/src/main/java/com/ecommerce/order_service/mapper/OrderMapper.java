package com.ecommerce.order_service.mapper;

import com.ecommerce.order_service.dto.OrderResponse;
import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct — Order entity → OrderResponse DTO dönüşümü.
 * Service katmanındaki inline toResponse() metodunun yerini alır.
 */
@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "id", target = "orderId")
    @Mapping(source = "status", target = "status")
    OrderResponse toResponse(Order order);

    @Mapping(source = "productId", target = "productId")
    @Mapping(source = "productName", target = "productName")
    OrderResponse.OrderItemResponse toItemResponse(OrderItem item);

    default String statusToString(com.ecommerce.order_service.entity.OrderStatus status) {
        return status != null ? status.name() : null;
    }
}
