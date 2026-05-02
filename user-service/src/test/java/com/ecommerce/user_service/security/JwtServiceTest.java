package com.ecommerce.user_service.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService Unit Testleri")
class JwtServiceTest {

    private JwtService jwtService;

    // Minimum 256-bit gizli anahtar (Base64 kodlu) gereklidir, aksi takdirde JJWT hata fırlatır.
    private final String SECRET_KEY = "VGhpcy1pcy1hLXZlcnktc2VjdXJlLWFuZC1sb25nLXNlY3JldC1rZXktZm9yLWp3dC10ZXN0aW5nIQ==";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "expiration", 1000 * 60 * 60); // 1 saat
    }

    @Test
    @DisplayName("Token başarıyla oluşturulur ve username çıkarılır")
    void generateToken_andExtractUsername_shouldWorkCorrectly() {
        // ACT
        String token = jwtService.generateToken("alper", "CUSTOMER");
        String extractedUsername = jwtService.extractUsername(token);

        // ASSERT
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(extractedUsername).isEqualTo("alper");
    }

    @Test
    @DisplayName("Geçerli token için isTokenValid true döner")
    void isTokenValid_withValidToken_shouldReturnTrue() {
        // ARRANGE
        String token = jwtService.generateToken("alper", "CUSTOMER");

        // ACT
        boolean isValid = jwtService.isTokenValid(token);

        // ASSERT
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Bozuk token için isTokenValid false döner")
    void isTokenValid_withInvalidToken_shouldReturnFalse() {
        // ACT
        boolean isValid = jwtService.isTokenValid("bozuk.bir.token");

        // ASSERT
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Süresi geçmiş token için isTokenValid false döner")
    void isTokenValid_withExpiredToken_shouldReturnFalse() {
        // ARRANGE: Expiration değerini negatif (geçmiş zaman) yaparak token üretiyoruz
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L); // -1 saniye
        String token = jwtService.generateToken("alper", "CUSTOMER");

        // ACT
        boolean isValid = jwtService.isTokenValid(token);

        // ASSERT
        assertThat(isValid).isFalse();
    }
}
