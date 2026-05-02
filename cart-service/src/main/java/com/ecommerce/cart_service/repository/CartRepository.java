package com.ecommerce.cart_service.repository;

import com.ecommerce.cart_service.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUsername(String username);

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.id = :id")
    Optional<Cart> findByIdWithItems(@Param("id") Long id);

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.username = :username")
    Optional<Cart> findByUsernameWithItems(@Param("username") String username);
}