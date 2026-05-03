package com.ecommerce.product_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotBlank(message = "Ürün adı boş olamaz")
    private String name;

    private String description;

    @NotNull(message = "Fiyat boş olamaz")
    @Min(value = 0, message = "Fiyat negatif olamaz")
    private BigDecimal price;

    @NotNull(message = "Stok boş olamaz")
    @Min(value = 0, message = "Stok negatif olamaz")
    private Integer stock;

    private String category;

    private java.util.List<String> images;
}