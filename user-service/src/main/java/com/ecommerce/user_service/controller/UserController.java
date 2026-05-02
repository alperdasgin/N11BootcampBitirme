package com.ecommerce.user_service.controller;

import com.ecommerce.user_service.dto.*;
import com.ecommerce.user_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User", description = "Kullanıcı işlemleri")
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    @Operation(summary = "Yeni kullanıcı kaydı")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    @PostMapping("/verify")
    @Operation(summary = "E-Posta OTP doğrulama")
    public ResponseEntity<AuthResponse> verify(@Valid @RequestBody com.ecommerce.user_service.dto.VerifyRequest request) {
        return ResponseEntity.ok(userService.verifyOtp(request));
    }

    @PostMapping("/signin")
    @Operation(summary = "Kullanıcı girişi")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Kullanıcı sil")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.deleteUser(userId));
    }

    @GetMapping("/health")
    @Operation(summary = "Servis sağlık kontrolü")
    public ResponseEntity<MessageResponse> health() {
        return ResponseEntity.ok(new MessageResponse("User Service çalışıyor"));
    }
}