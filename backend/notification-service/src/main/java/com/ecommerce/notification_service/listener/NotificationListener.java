package com.ecommerce.notification_service.listener;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final JavaMailSender mailSender;

    // ── Sipariş Tamamlandı → Sipariş Onay Maili ──────────────────────────────
    @RabbitListener(bindings = @QueueBinding(
            value    = @Queue(value = "${notification.rabbit.queue}", durable = "true"),
            exchange = @Exchange(value = "${notification.rabbit.exchange}", type = "topic"),
            key      = "${notification.rabbit.routingKey}"
    ))
    public void onOrderCompleted(OrderCompletedEvent event) {
        log.info("OrderCompletedEvent alındı. Sipariş No: {}, E-Posta: {}", event.getOrderId(), event.getEmail());

        if (event.getEmail() == null || event.getEmail().isEmpty()) {
            log.warn("E-Posta adresi bulunamadığı için mail gönderilemedi.");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("ecommercealperdasgin@gmail.com");
            message.setTo(event.getEmail());
            message.setSubject("ShopApp - Siparişiniz Başarıyla Alındı! 🎉");
            message.setText("Merhaba " + event.getFirstName() + ",\n\n"
                    + "#" + event.getOrderId() + " numaralı siparişiniz başarıyla alınmış ve onaylanmıştır.\n"
                    + "Toplam Tutar: ₺" + event.getTotalPrice() + "\n\n"
                    + "Bizi tercih ettiğiniz için teşekkür ederiz.\n"
                    + "ShopApp Ekibi");

            mailSender.send(message);
            log.info("E-Posta başarıyla gönderildi: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Sipariş e-postası gönderilirken hata: {}", e.getMessage());
        }
    }

    // ── Kullanıcı Kayıt Oldu → OTP Doğrulama Maili ───────────────────────────
    @RabbitListener(bindings = @QueueBinding(
            value    = @Queue(value = "user.otp.queue", durable = "true"),
            exchange = @Exchange(value = "user.exchange", type = "topic"),
            key      = "user.registered"
    ))
    public void onUserRegistered(Map<String, Object> event) {
        String email = (String) event.get("email");
        String name  = (String) event.get("name");
        String otp   = (String) event.get("otpCode");

        log.info("UserRegisteredEvent alındı. email={}, otp={}", email, otp);

        if (email == null || otp == null) {
            log.warn("OTP maili için gerekli bilgiler eksik.");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("ecommercealperdasgin@gmail.com");
            message.setTo(email);
            message.setSubject("ShopApp - E-Posta Doğrulama Kodunuz 🔑");
            message.setText("Merhaba " + name + ",\n\n"
                    + "ShopApp'a hoş geldiniz!\n\n"
                    + "Hesabınızı doğrulamak için aşağıdaki 6 haneli kodu kullanın:\n\n"
                    + "━━━━━━━━━━━━━━━━\n"
                    + "  🔑  " + otp + "\n"
                    + "━━━━━━━━━━━━━━━━\n\n"
                    + "Bu kod 15 dakika geçerlidir.\n\n"
                    + "Eğer bu işlemi siz yapmadıysanız bu e-postayı dikkate almayın.\n\n"
                    + "ShopApp Ekibi");

            mailSender.send(message);
            log.info("OTP maili başarıyla gönderildi: {}", email);
        } catch (Exception e) {
            log.error("OTP maili gönderilirken hata: {}", e.getMessage());
        }
    }

    // ── Inner DTO ─────────────────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderCompletedEvent {
        private Long orderId;
        private String email;
        private String firstName;
        private Double totalPrice;
    }
}
