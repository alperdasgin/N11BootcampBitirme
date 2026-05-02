package com.ecommerce.user_service.service;

import com.ecommerce.user_service.dto.*;
import com.ecommerce.user_service.entity.User;
import com.ecommerce.user_service.entity.VerificationToken;
import com.ecommerce.user_service.repository.UserRepository;
import com.ecommerce.user_service.repository.VerificationTokenRepository;
import com.ecommerce.user_service.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final VerificationTokenRepository verificationTokenRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${otp.exchange}")
    private String otpExchange;

    @Value("${otp.routing-key}")
    private String otpRoutingKey;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Yeni kullanıcı kaydı: username={}, email={}", request.getUsername(), request.getEmail());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Bu kullanıcı adı zaten kullanılıyor: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Bu email zaten kayıtlı: " + request.getEmail());
        }

        // Kullanıcıyı isVerified=false olarak kaydet
        User user = User.builder()
                .name(request.getName())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.CUSTOMER)
                .isVerified(false)
                .build();

        User saved = userRepository.save(user);
        log.info("Kullanıcı kaydedildi (doğrulama bekleniyor). id={}", saved.getId());

        // 6 haneli OTP kodu üret
        String otpCode = String.format("%06d", new Random().nextInt(999999));

        // OTP'yi veritabanına kaydet (15 dakika geçerli)
        VerificationToken token = VerificationToken.builder()
                .email(saved.getEmail())
                .code(otpCode)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        verificationTokenRepository.save(token);
        log.info("OTP oluşturuldu. email={}, code={}", saved.getEmail(), otpCode);

        // Notification service'e RabbitMQ ile mesaj gönder
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("email", saved.getEmail());
            event.put("name", saved.getName());
            event.put("otpCode", otpCode);
            rabbitTemplate.convertAndSend(otpExchange, otpRoutingKey, event);
            log.info("OTP e-posta eventi yayınlandı. email={}", saved.getEmail());
        } catch (Exception e) {
            log.error("OTP eventi gönderilemedi: {}", e.getMessage());
        }

        // Token OLMADAN yanıt döndür (kullanıcı henüz doğrulanmadı)
        return AuthResponse.builder()
                .token(null)
                .tokenType("Bearer")
                .userId(saved.getId())
                .name(saved.getName())
                .username(saved.getUsername())
                .email(saved.getEmail())
                .role(saved.getRole().name())
                .requiresVerification(true)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse verifyOtp(VerifyRequest request) {
        log.info("OTP doğrulama denemesi. email={}", request.getEmail());

        VerificationToken token = verificationTokenRepository
                .findTopByEmailOrderByIdDesc(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Bu e-posta için doğrulama kodu bulunamadı."));

        if (token.isUsed()) {
            throw new RuntimeException("Bu doğrulama kodu daha önce kullanılmış.");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Doğrulama kodunun süresi dolmuş. Lütfen tekrar kayıt olun.");
        }
        if (!token.getCode().equals(request.getCode())) {
            throw new RuntimeException("Doğrulama kodu hatalı.");
        }

        // Kodu kullanıldı olarak işaretle
        token.setUsed(true);
        verificationTokenRepository.save(token);

        // Kullanıcıyı doğrulandı olarak işaretle
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));
        user.setIsVerified(true);
        userRepository.save(user);

        log.info("Kullanıcı doğrulandı. username={}", user.getUsername());

        // Artık JWT token ver
        String jwtToken = jwtService.generateToken(user.getUsername(), user.getRole().name());

        return AuthResponse.builder()
                .token(jwtToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .requiresVerification(false)
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Giriş denemesi: {}", request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
                .or(() -> userRepository.findByEmail(request.getUsername()))
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + request.getUsername()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Şifre hatalı");
        }

        if (Boolean.FALSE.equals(user.getIsVerified())) {
            throw new RuntimeException("Hesabınız henüz doğrulanmamış. Lütfen e-postanıza gelen kodu girin.");
        }

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        log.info("Giriş başarılı: username={}", user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .requiresVerification(false)
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
