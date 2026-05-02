package com.ecommerce.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Ad Soyad boş olamaz")
    @Size(min = 2, max = 100, message = "Ad Soyad en az 2 karakter olmalı")
    private String name;        // "Alper Daşgın" gibi tam ad

    @NotBlank(message = "Kullanıcı adı boş olamaz")
    @Size(min = 3, max = 50, message = "Kullanıcı adı 3-50 karakter olmalı")
    private String username;    // Login için kullanılır (URL-safe tutulmalı)

    @NotBlank(message = "Email boş olamaz")
    @Email(message = "Geçerli bir email girin")
    private String email;

    @NotBlank(message = "Şifre boş olamaz")
    @Size(min = 6, max = 40, message = "Şifre 6-40 karakter olmalı")
    private String password;
}
