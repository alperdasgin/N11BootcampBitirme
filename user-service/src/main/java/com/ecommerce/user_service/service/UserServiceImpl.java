package com.ecommerce.user_service.service;

import com.ecommerce.user_service.dto.*;
import com.ecommerce.user_service.entity.User;
import com.ecommerce.user_service.repository.UserRepository;
import com.ecommerce.user_service.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public AuthResponse register(RegisterRequest request) {
        log.info("Yeni kullanıcı kaydı: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Bu kullanıcı adı zaten kullanılıyor: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Bu email zaten kayıtlı: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.CUSTOMER)
                .build();

        User saved = userRepository.save(user);
        log.info("Kullanıcı kaydedildi. id={}", saved.getId());

        String token = jwtService.generateToken(saved.getUsername(), saved.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(saved.getId())
                .username(saved.getUsername())
                .email(saved.getEmail())
                .role(saved.getRole().name())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Giriş denemesi: {}", request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + request.getUsername()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Şifre hatalı");
        }

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        log.info("Giriş başarılı: {}", user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public MessageResponse deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + userId));
        userRepository.delete(user);
        log.info("Kullanıcı silindi. id={}", userId);
        return new MessageResponse("Kullanıcı başarıyla silindi");
    }
}
