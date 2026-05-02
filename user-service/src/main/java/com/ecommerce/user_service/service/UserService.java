package com.ecommerce.user_service.service;

import com.ecommerce.user_service.dto.AuthResponse;
import com.ecommerce.user_service.dto.LoginRequest;
import com.ecommerce.user_service.dto.MessageResponse;
import com.ecommerce.user_service.dto.RegisterRequest;

public interface UserService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    MessageResponse deleteUser(Long userId);
}
