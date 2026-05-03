package com.ecommerce.cart_service.controller;

import com.ecommerce.cart_service.dto.*;
import com.ecommerce.cart_service.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Cart", description = "Sepet işlemleri")
public class CartController {

    private final CartService cartService;

    @GetMapping("/{username}")
    @Operation(summary = "Kullanıcının sepetini getir")
    public ResponseEntity<CartResponse> getCart(@PathVariable String username) {
        return ResponseEntity.ok(cartService.getCart(username));
    }

    @PostMapping("/{username}/add")
    @Operation(summary = "Sepete ürün ekle")
    public ResponseEntity<CartResponse> addToCart(
            @PathVariable String username,
            @Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addToCart(username, request));
    }

    @DeleteMapping("/{username}/remove/{productId}")
    @Operation(summary = "Sepetten ürün çıkar")
    public ResponseEntity<CartResponse> removeFromCart(
            @PathVariable String username,
            @PathVariable Long productId) {
        return ResponseEntity.ok(cartService.removeFromCart(username, productId));
    }

    @PutMapping("/{username}/update/{productId}")
    @Operation(summary = "Ürün miktarını güncelle")
    public ResponseEntity<CartResponse> updateQuantity(
            @PathVariable String username,
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(cartService.updateQuantity(username, productId, quantity));
    }

    @DeleteMapping("/{username}/clear")
    @Operation(summary = "Sepeti temizle")
    public ResponseEntity<Void> clearCart(@PathVariable String username) {
        cartService.clearCart(username);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Cart Service çalışıyor");
    }
}