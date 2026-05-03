package com.ecommerce.user_service.service;

import com.ecommerce.user_service.dto.AuthResponse;
import com.ecommerce.user_service.dto.LoginRequest;
import com.ecommerce.user_service.dto.MessageResponse;
import com.ecommerce.user_service.dto.RegisterRequest;
import com.ecommerce.user_service.entity.User;
import com.ecommerce.user_service.repository.UserRepository;
import com.ecommerce.user_service.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Unit Testleri")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserServiceImpl userService;

    // ─────────────────────────────────────────────────────────────
    // register Testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Geçerli verilerle kayıt başarılı olur ve token döner")
    void register_withValidRequest_shouldSaveUserAndReturnToken() {
        // ARRANGE
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alper");
        req.setEmail("alper@test.com");
        req.setPassword("password123");
        req.setName("Alper Daşgın");

        when(userRepository.existsByUsername("alper")).thenReturn(false);
        when(userRepository.existsByEmail("alper@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        
        User savedUser = User.builder()
                .id(1L).username("alper").email("alper@test.com").role(User.Role.CUSTOMER).name("Alper Daşgın").build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken("alper", "CUSTOMER")).thenReturn("dummy-jwt-token");

        // ACT
        AuthResponse response = userService.register(req);

        // ASSERT
        assertThat(response.getToken()).isEqualTo("dummy-jwt-token");
        assertThat(response.getUsername()).isEqualTo("alper");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Aynı username ile kayıt olmaya çalışılırsa hata fırlatır")
    void register_withExistingUsername_shouldThrowException() {
        // ARRANGE
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alper");
        req.setEmail("alper@test.com");
        
        when(userRepository.existsByUsername("alper")).thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("zaten kullanılıyor");
        verify(userRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────
    // login Testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Geçerli username ve şifre ile giriş yapılır")
    void login_withValidUsernameAndPassword_shouldReturnToken() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alper");
        req.setPassword("password123");
        User user = User.builder().id(1L).username("alper").password("encoded").role(User.Role.CUSTOMER).build();
        
        when(userRepository.findByUsername("alper")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);
        when(jwtService.generateToken("alper", "CUSTOMER")).thenReturn("dummy-token");

        // ACT
        AuthResponse response = userService.login(req);

        // ASSERT
        assertThat(response.getToken()).isEqualTo("dummy-token");
        assertThat(response.getUsername()).isEqualTo("alper");
    }

    @Test
    @DisplayName("Geçerli email ve şifre ile giriş yapılır")
    void login_withValidEmailAndPassword_shouldReturnToken() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alper@test.com");
        req.setPassword("password123");
        User user = User.builder().id(1L).username("alper").email("alper@test.com").password("encoded").role(User.Role.CUSTOMER).build();
        
        when(userRepository.findByUsername("alper@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("alper@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);
        when(jwtService.generateToken("alper", "CUSTOMER")).thenReturn("dummy-token");

        // ACT
        AuthResponse response = userService.login(req);

        // ASSERT
        assertThat(response.getToken()).isEqualTo("dummy-token");
        assertThat(response.getUsername()).isEqualTo("alper"); // yanıt username dönmeli
    }

    @Test
    @DisplayName("Hatalı şifre ile girişte exception fırlatır")
    void login_withWrongPassword_shouldThrowException() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alper");
        req.setPassword("wrongPass");
        User user = User.builder().id(1L).username("alper").password("encoded").build();
        
        when(userRepository.findByUsername("alper")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "encoded")).thenReturn(false);

        // ACT & ASSERT
        assertThatThrownBy(() -> userService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Şifre hatalı");
    }

    // ─────────────────────────────────────────────────────────────
    // deleteUser Testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Var olan kullanıcı ID ile başarıyla silinir")
    void deleteUser_withExistingId_shouldDelete() {
        // ARRANGE
        User user = User.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // ACT
        MessageResponse response = userService.deleteUser(1L);

        // ASSERT
        assertThat(response.getMessage()).contains("başarıyla silindi");
        verify(userRepository, times(1)).delete(user);
    }
}
