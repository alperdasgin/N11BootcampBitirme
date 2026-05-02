package com.ecommerce.cart_service.service;

import com.ecommerce.cart_service.dto.*;
import com.ecommerce.cart_service.entity.*;
import com.ecommerce.cart_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    // Sepeti getir veya oluştur (items olmadan — sadece ID için)
    private Cart getOrCreateCart(String username) {
        return cartRepository.findByUsername(username)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder().username(username).build();
                    return cartRepository.save(newCart);
                });
    }

    // Sepete ürün ekle
    @Transactional
    public CartResponse addToCart(String username, AddToCartRequest request) {
        Cart cart = getOrCreateCart(username);

        Optional<CartItem> existingItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), request.getProductId());

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
            log.info("Sepetteki ürün güncellendi. productId={}", request.getProductId());
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productId(request.getProductId())
                    .productName(request.getProductName())
                    .price(request.getPrice())
                    .quantity(request.getQuantity())
                    .imageUrl(request.getImageUrl())
                    .build();
            cartItemRepository.save(newItem);
            log.info("Sepete yeni ürün eklendi. productId={}", request.getProductId());
        }

        // JOIN FETCH ile taze veri çek — Hibernate L1 cache bypass
        return toResponse(cartRepository.findByIdWithItems(cart.getId()).orElseThrow());
    }

    // Sepeti getir
    @Transactional(readOnly = true)
    public CartResponse getCart(String username) {
        Cart cart = cartRepository.findByUsernameWithItems(username)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder().username(username).build();
                    return cartRepository.save(newCart);
                });
        return toResponse(cart);
    }

    // Sepetten ürün çıkar
    @Transactional
    public CartResponse removeFromCart(String username, Long productId) {
        Cart cart = cartRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Sepet bulunamadı"));
        cartItemRepository.deleteByCartIdAndProductId(cart.getId(), productId);
        log.info("Sepetten ürün çıkarıldı. productId={}", productId);
        return toResponse(cartRepository.findByIdWithItems(cart.getId()).orElseThrow());
    }

    // Ürün miktarını güncelle
    @Transactional
    public CartResponse updateQuantity(String username, Long productId, Integer quantity) {
        Cart cart = cartRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Sepet bulunamadı"));

        if (quantity <= 0) {
            cartItemRepository.deleteByCartIdAndProductId(cart.getId(), productId);
        } else {
            CartItem item = cartItemRepository
                    .findByCartIdAndProductId(cart.getId(), productId)
                    .orElseThrow(() -> new RuntimeException("Ürün sepette bulunamadı"));
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }
        return toResponse(cartRepository.findByIdWithItems(cart.getId()).orElseThrow());
    }

    // Sepeti temizle
    @Transactional
    public void clearCart(String username) {
        Cart cart = cartRepository.findByUsernameWithItems(username)
                .orElseThrow(() -> new RuntimeException("Sepet bulunamadı"));
        cart.getItems().clear();
        cartRepository.save(cart);
        log.info("Sepet temizlendi. username={}", username);
    }

    // Entity → DTO
    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(item -> CartItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .imageUrl(item.getImageUrl())
                        .subtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .toList();

        return CartResponse.builder()
                .id(cart.getId())
                .username(cart.getUsername())
                .items(items)
                .totalPrice(cart.getTotalPrice())
                .totalItems(items.stream().mapToInt(CartItemResponse::getQuantity).sum())
                .build();
    }
}