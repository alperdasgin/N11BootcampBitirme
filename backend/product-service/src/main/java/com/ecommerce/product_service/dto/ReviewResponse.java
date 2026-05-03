package com.ecommerce.product_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {
    private Long id;
    private String username;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
