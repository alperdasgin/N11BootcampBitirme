package com.ecommerce.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyRequest {

    @NotBlank(message = "Email boş olamaz")
    @Email(message = "Geçerli bir email girin")
    private String email;

    @NotBlank(message = "Doğrulama kodu boş olamaz")
    private String code;
}
