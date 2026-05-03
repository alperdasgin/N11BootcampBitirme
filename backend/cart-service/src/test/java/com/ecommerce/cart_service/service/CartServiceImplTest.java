package com.ecommerce.cart_service.service;

import com.ecommerce.cart_service.dto.AddToCartRequest;
import com.ecommerce.cart_service.dto.CartResponse;
import com.ecommerce.cart_service.entity.Cart;
import com.ecommerce.cart_service.entity.CartItem;
import com.ecommerce.cart_service.repository.CartItemRepository;
import com.ecommerce.cart_service.repository.CartRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartServiceImpl Unit Testleri")
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    // ─────────────────────────────────────────────────────────────
    // addToCart Testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Sepette olmayan ürün eklendiğinde yeni CartItem oluşturulur")
    void addToCart_withNewProduct_shouldCreateNewCartItem() {
        // ARRANGE
        Cart cart = buildCart(1L, "alper");
        when(cartRepository.findByUsername("alper")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 100L)).thenReturn(Optional.empty());
        when(cartRepository.findByIdWithItems(1L)).thenReturn(Optional.of(cart));

        AddToCartRequest req = new AddToCartRequest();
        req.setProductId(100L);
        req.setProductName("Laptop");
        req.setPrice(new BigDecimal("15000"));
        req.setQuantity(2);

        // ACT
        CartResponse response = cartService.addToCart("alper", req);

        // ASSERT
        verify(cartItemRepository, times(1)).save(argThat(item -> 
                item.getProductId().equals(100L) && item.getQuantity().equals(2)));
        assertThat(response.getUsername()).isEqualTo("alper");
    }

    @Test
    @DisplayName("Sepette zaten olan ürün eklendiğinde miktarı artırılır")
    void addToCart_withExistingProduct_shouldIncreaseQuantity() {
        // ARRANGE
        Cart cart = buildCart(1L, "alper");
        CartItem existingItem = CartItem.builder()
                .id(10L).cart(cart).productId(100L).quantity(2).price(new BigDecimal("15000")).build();
        
        when(cartRepository.findByUsername("alper")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 100L)).thenReturn(Optional.of(existingItem));
        when(cartRepository.findByIdWithItems(1L)).thenReturn(Optional.of(cart));

        AddToCartRequest req = new AddToCartRequest();
        req.setProductId(100L);
        req.setQuantity(3);

        // ACT
        cartService.addToCart("alper", req);

        // ASSERT
        verify(cartItemRepository, times(1)).save(argThat(item -> 
                item.getQuantity().equals(5))); // 2 (eski) + 3 (yeni)
    }

    // ─────────────────────────────────────────────────────────────
    // getCart Testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Kullanıcının sepeti varsa doğru şekilde döner")
    void getCart_whenExists_shouldReturnCart() {
        // ARRANGE
        Cart cart = buildCart(1L, "alper");
        CartItem item1 = CartItem.builder().productId(10L).quantity(2).price(new BigDecimal("100")).build();
        cart.setItems(List.of(item1));

        when(cartRepository.findByUsernameWithItems("alper")).thenReturn(Optional.of(cart));

        // ACT
        CartResponse response = cartService.getCart("alper");

        // ASSERT
        assertThat(response.getUsername()).isEqualTo("alper");
        assertThat(response.getTotalItems()).isEqualTo(2);
        assertThat(response.getTotalPrice()).isEqualTo(new BigDecimal("200")); // 2 * 100
    }

    @Test
    @DisplayName("Kullanıcının sepeti yoksa yeni boş sepet oluşturulup döner")
    void getCart_whenNotExists_shouldCreateAndReturnEmptyCart() {
        // ARRANGE
        when(cartRepository.findByUsernameWithItems("ahmet")).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> {
            Cart c = i.getArgument(0);
            c.setId(99L);
            c.setItems(new ArrayList<>());
            return c;
        });

        // ACT
        CartResponse response = cartService.getCart("ahmet");

        // ASSERT
        assertThat(response.getUsername()).isEqualTo("ahmet");
        assertThat(response.getTotalItems()).isEqualTo(0);
        assertThat(response.getTotalPrice()).isEqualTo(BigDecimal.ZERO);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    // ─────────────────────────────────────────────────────────────
    // removeFromCart Testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Sepetten ürün silme işlemi başarılı olur")
    void removeFromCart_shouldDeleteCartItem() {
        // ARRANGE
        Cart cart = buildCart(1L, "alper");
        when(cartRepository.findByUsername("alper")).thenReturn(Optional.of(cart));
        when(cartRepository.findByIdWithItems(1L)).thenReturn(Optional.of(cart));

        // ACT
        cartService.removeFromCart("alper", 100L);

        // ASSERT
        verify(cartItemRepository, times(1)).deleteByCartIdAndProductId(1L, 100L);
    }

    // ─────────────────────────────────────────────────────────────
    // updateQuantity Testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Miktar güncelleme işlemi başarılı olur")
    void updateQuantity_withPositiveQuantity_shouldUpdateItem() {
        // ARRANGE
        Cart cart = buildCart(1L, "alper");
        CartItem item = CartItem.builder().id(10L).cart(cart).productId(100L).quantity(2).build();

        when(cartRepository.findByUsername("alper")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 100L)).thenReturn(Optional.of(item));
        when(cartRepository.findByIdWithItems(1L)).thenReturn(Optional.of(cart));

        // ACT
        cartService.updateQuantity("alper", 100L, 5);

        // ASSERT
        verify(cartItemRepository, times(1)).save(argThat(i -> i.getQuantity().equals(5)));
        verify(cartItemRepository, never()).deleteByCartIdAndProductId(any(), any());
    }

    @Test
    @DisplayName("Miktar 0 veya negatif verilirse ürün sepetten silinir")
    void updateQuantity_withZeroOrNegativeQuantity_shouldRemoveItem() {
        // ARRANGE
        Cart cart = buildCart(1L, "alper");
        when(cartRepository.findByUsername("alper")).thenReturn(Optional.of(cart));
        when(cartRepository.findByIdWithItems(1L)).thenReturn(Optional.of(cart));

        // ACT
        cartService.updateQuantity("alper", 100L, 0);

        // ASSERT
        verify(cartItemRepository, times(1)).deleteByCartIdAndProductId(1L, 100L);
        verify(cartItemRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────
    // clearCart Testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Sepeti tamamen temizler")
    void clearCart_shouldRemoveAllItems() {
        // ARRANGE
        Cart cart = buildCart(1L, "alper");
        List<CartItem> items = new ArrayList<>();
        items.add(CartItem.builder().id(10L).build());
        cart.setItems(items);

        when(cartRepository.findByUsernameWithItems("alper")).thenReturn(Optional.of(cart));

        // ACT
        cartService.clearCart("alper");

        // ASSERT
        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository, times(1)).save(cart);
    }

    // ─────────────────────────────────────────────────────────────
    // Yardımcı Metot
    // ─────────────────────────────────────────────────────────────

    private Cart buildCart(Long id, String username) {
        Cart cart = new Cart();
        cart.setId(id);
        cart.setUsername(username);
        cart.setItems(new ArrayList<>());
        return cart;
    }
}
