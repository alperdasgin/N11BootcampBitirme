package com.ecommerce.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType = "Bearer";
    private Long userId;
    private String name;        // Görünen ad: "Alper Daşgın"
    private String username;    // Login id
    private String email;
    private String role;
    private Boolean requiresVerification;
}