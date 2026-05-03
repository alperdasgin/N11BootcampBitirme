package com.ecommerce.user_service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

// Tam Spring context'i ayağa kaldırmak için gerçek DB ve RabbitMQ gerekir.
// Integration testleri için ayrı bir profil/ortam kullanılmalıdır.
@Disabled("Integration test — gerçek altyapı gerektirir")
class UserServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
