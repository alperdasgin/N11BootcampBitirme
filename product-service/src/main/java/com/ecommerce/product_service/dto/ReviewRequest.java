package com.ecommerce.product_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotBlank(message = "Kullanıcı adı boş olamaz")
    private String username;

    @Min(value = 1, message = "Puan en az 1 olmalıdır")
    @Max(value = 5, message = "Puan en fazla 5 olmalıdır")
    private Integer rating;

    private String comment;
}
