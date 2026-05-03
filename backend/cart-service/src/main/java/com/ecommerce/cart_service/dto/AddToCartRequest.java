package com.ecommerce.cart_service.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class AddToCartRequest {
    @NotNull private Long productId;
    @NotBlank private String productName;
    @NotNull @Min(0) private BigDecimal price;
    @NotNull @Min(1) private Integer quantity;
    private String imageUrl;
}