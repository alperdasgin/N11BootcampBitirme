package com.ecommerce.user_service.service;

import com.ecommerce.user_service.dto.AuthResponse;
import com.ecommerce.user_service.dto.LoginRequest;
import com.ecommerce.user_service.dto.MessageResponse;
import com.ecommerce.user_service.dto.RegisterRequest;
import com.ecommerce.user_service.dto.VerifyRequest;

public interface UserService {

    AuthResponse register(RegisterRequest request);

    AuthResponse verifyOtp(VerifyRequest request);

    AuthResponse login(LoginRequest request);

    MessageResponse deleteUser(Long userId);
}
