package com.ecommerce.order_service.repository;

import com.ecommerce.order_service.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUsername(String username);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items LEFT JOIN FETCH o.orderDetails WHERE o.id = :id")
    Optional<Order> findWithItemsAndDetails(@Param("id") Long id);
}
