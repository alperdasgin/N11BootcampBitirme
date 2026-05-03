package com.ecommerce.user_service.service;

import com.ecommerce.user_service.dto.AuthResponse;
import com.ecommerce.user_service.dto.LoginRequest;
import com.ecommerce.user_service.dto.MessageResponse;
import com.ecommerce.user_service.dto.RegisterRequest;
import com.ecommerce.user_service.entity.User;
import com.ecommerce.user_service.entity.VerificationToken;
import com.ecommerce.user_service.repository.UserRepository;
import com.ecommerce.user_service.repository.VerificationTokenRepository;
import com.ecommerce.user_service.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Unit Testleri")
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private VerificationTokenRepository verificationTokenRepository;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private UserServiceImpl userService;

    // ─────────────────────────────────────────────────────────────
    // register Testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Geçerli verilerle kayıt başarılı olur, OTP doğrulama beklenir")
    void register_withValidRequest_shouldSaveUserAndRequireVerification() {
        // ARRANGE
        ReflectionTestUtils.setField(userService, "otpExchange", "user.exchange");
        ReflectionTestUtils.setField(userService, "otpRoutingKey", "user.registered");

        RegisterRequest req = new RegisterRequest();
        req.setUsername("alper");
        req.setEmail("alper@test.com");
        req.setPassword("password123");
        req.setName("Alper Daşgın");

        when(userRepository.existsByUsername("alper")).thenReturn(false);
        when(userRepository.existsByEmail("alper@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        User savedUser = User.builder()
                .id(1L).username("alper").email("alper@test.com")
                .role(User.Role.CUSTOMER).name("Alper Daşgın").isVerified(false).build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(verificationTokenRepository.save(any(VerificationToken.class))).thenReturn(null);

        // ACT
        AuthResponse response = userService.register(req);

        // ASSERT
        assertThat(response.getUsername()).isEqualTo("alper");
        assertThat(response.getRequiresVerification()).isTrue();
        assertThat(response.getToken()).isNull(); // OTP doğrulanmadan token verilmez
        verify(userRepository, times(1)).save(any(User.class));
        verify(verificationTokenRepository, times(1)).save(any(VerificationToken.class));
    }

    @Test
    @DisplayName("Aynı username ile kayıt olmaya çalışılırsa hata fırlatır")
    void register_withExistingUsername_shouldThrowException() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alper");
        req.setEmail("alper@test.com");

        when(userRepository.existsByUsername("alper")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("zaten kullanılıyor");
        verify(userRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────
    // login Testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Doğrulanmış kullanıcı username ile giriş yapabilir")
    void login_withVerifiedUserAndUsername_shouldReturnToken() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alper");
        req.setPassword("password123");

        User user = User.builder()
                .id(1L).username("alper").password("encoded")
                .role(User.Role.CUSTOMER).isVerified(true).build();

        when(userRepository.findByUsername("alper")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);
        when(jwtService.generateToken("alper", "CUSTOMER")).thenReturn("dummy-token");

        AuthResponse response = userService.login(req);

        assertThat(response.getToken()).isEqualTo("dummy-token");
        assertThat(response.getUsername()).isEqualTo("alper");
    }

    @Test
    @DisplayName("Doğrulanmış kullanıcı email ile giriş yapabilir")
    void login_withVerifiedUserAndEmail_shouldReturnToken() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alper@test.com");
        req.setPassword("password123");

        User user = User.builder()
                .id(1L).username("alper").email("alper@test.com")
                .password("encoded").role(User.Role.CUSTOMER).isVerified(true).build();

        when(userRepository.findByUsername("alper@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("alper@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);
        when(jwtService.generateToken("alper", "CUSTOMER")).thenReturn("dummy-token");

        AuthResponse response = userService.login(req);

        assertThat(response.getToken()).isEqualTo("dummy-token");
        assertThat(response.getUsername()).isEqualTo("alper");
    }

    @Test
    @DisplayName("Doğrulanmamış kullanıcı giriş yapamaz")
    void login_withUnverifiedUser_shouldThrowException() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alper");
        req.setPassword("password123");

        User user = User.builder()
                .id(1L).username("alper").password("encoded")
                .role(User.Role.CUSTOMER).isVerified(false).build();

        when(userRepository.findByUsername("alper")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);

        assertThatThrownBy(() -> userService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("doğrulanmamış");
    }

    @Test
    @DisplayName("Hatalı şifre ile girişte exception fırlatır")
    void login_withWrongPassword_shouldThrowException() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alper");
        req.setPassword("wrongPass");

        User user = User.builder()
                .id(1L).username("alper").password("encoded").isVerified(true).build();

        when(userRepository.findByUsername("alper")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Şifre hatalı");
    }

    // ─────────────────────────────────────────────────────────────
    // deleteUser Testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Var olan kullanıcı başarıyla silinir")
    void deleteUser_withExistingId_shouldDelete() {
        User user = User.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        MessageResponse response = userService.deleteUser(1L);

        assertThat(response.getMessage()).contains("başarıyla silindi");
        verify(userRepository, times(1)).delete(user);
    }
}
