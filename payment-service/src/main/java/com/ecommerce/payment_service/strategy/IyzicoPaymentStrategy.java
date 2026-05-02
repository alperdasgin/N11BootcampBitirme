package com.ecommerce.payment_service.strategy;

import com.ecommerce.payment_service.dto.PaymentRequest;
import com.ecommerce.payment_service.dto.PaymentResponse;
import com.iyzipay.Options;
import com.iyzipay.model.*;
import com.iyzipay.request.CreatePaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Strategy Pattern — Iyzico implementasyonu.
 * Farklı bir ödeme sağlayıcısı eklemek için yeni bir @Component yaz
 * ve PaymentStrategy interface'ini implemente et.
 */
@Component
@Slf4j
public class IyzicoPaymentStrategy implements PaymentStrategy {

    @Value("${iyzico.api-key}")    private String apiKey;
    @Value("${iyzico.secret-key}") private String secretKey;
    @Value("${iyzico.base-url}")   private String baseUrl;

    @Override
    public PaymentResponse pay(PaymentRequest request) {
        log.info("[Iyzico] Ödeme başlatıldı. orderId={}", request.getOrderId());

        Options options = buildOptions();

        CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
        paymentRequest.setLocale(Locale.TR.getValue());
        paymentRequest.setConversationId(UUID.randomUUID().toString());
        paymentRequest.setPrice(BigDecimal.valueOf(request.getAmount()));
        paymentRequest.setPaidPrice(BigDecimal.valueOf(request.getAmount()));
        paymentRequest.setCurrency(Currency.TRY.name());
        paymentRequest.setInstallment(1);
        paymentRequest.setBasketId("ORDER-" + request.getOrderId());
        paymentRequest.setPaymentChannel(PaymentChannel.WEB.name());
        paymentRequest.setPaymentGroup(PaymentGroup.PRODUCT.name());
        paymentRequest.setPaymentCard(buildCard(request.getCard()));
        paymentRequest.setBuyer(buildBuyer(request));
        paymentRequest.setShippingAddress(buildAddress(request));
        paymentRequest.setBillingAddress(buildAddress(request));
        paymentRequest.setBasketItems(buildBasketItems(request));

        com.iyzipay.model.Payment result = com.iyzipay.model.Payment.create(paymentRequest, options);
        boolean success = "success".equalsIgnoreCase(result.getStatus());

        log.info("[Iyzico] Ödeme sonucu: success={}, paymentId={}", success, result.getPaymentId());

        return PaymentResponse.builder()
                .success(success)
                .transactionId(result.getPaymentId())
                .message(success ? "Ödeme başarılı" : result.getErrorMessage())
                .orderId(request.getOrderId())
                .build();
    }

    private Options buildOptions() {
        Options options = new Options();
        options.setApiKey(apiKey);
        options.setSecretKey(secretKey);
        options.setBaseUrl(baseUrl);
        return options;
    }

    private PaymentCard buildCard(PaymentRequest.Card card) {
        PaymentCard pc = new PaymentCard();
        pc.setCardHolderName(card.getCardHolderName());
        
        // Remove spaces from card number to prevent Iyzico test card validation errors
        String cleanCardNumber = card.getCardNumber() != null ? card.getCardNumber().replaceAll("\\s+", "") : null;
        log.info("[Iyzico] Kart bilgisi -> cardNumber='{}', expireMonth='{}', expireYear='{}'",
                cleanCardNumber, card.getExpireMonth(), card.getExpireYear());
        pc.setCardNumber(cleanCardNumber);
        
        pc.setExpireMonth(card.getExpireMonth());
        pc.setExpireYear(card.getExpireYear());
        pc.setCvc(card.getCvc());
        pc.setRegisterCard(0);
        return pc;
    }

    private Buyer buildBuyer(PaymentRequest r) {
        Buyer buyer = new Buyer();
        buyer.setId("USER-" + r.getUsername());
        buyer.setName(r.getFirstName());
        buyer.setSurname(r.getLastName());
        buyer.setEmail(r.getEmail());
        buyer.setIdentityNumber("74300864791");
        buyer.setRegistrationAddress(r.getAddress());
        buyer.setCity(r.getCity());
        buyer.setCountry(r.getCountry());
        buyer.setIp("127.0.0.1");
        return buyer;
    }

    private Address buildAddress(PaymentRequest r) {
        Address address = new Address();
        address.setContactName(r.getFirstName() + " " + r.getLastName());
        address.setCity(r.getCity());
        address.setCountry(r.getCountry());
        address.setAddress(r.getAddress() != null ? r.getAddress() : r.getCity());
        return address;
    }

    private List<BasketItem> buildBasketItems(PaymentRequest r) {
        List<BasketItem> items = new ArrayList<>();
        if (r.getItems() != null && !r.getItems().isEmpty()) {
            for (PaymentRequest.Item item : r.getItems()) {
                BasketItem bi = new BasketItem();
                bi.setId("PRODUCT-" + item.getProductId());
                bi.setName(item.getProductName());
                bi.setCategory1("Genel");
                bi.setItemType(BasketItemType.PHYSICAL.name());
                bi.setPrice(BigDecimal.valueOf(item.getPrice() * item.getQuantity()));
                items.add(bi);
            }
        } else {
            BasketItem bi = new BasketItem();
            bi.setId("ORDER-" + r.getOrderId());
            bi.setName("Sipariş #" + r.getOrderId());
            bi.setCategory1("Genel");
            bi.setItemType(BasketItemType.PHYSICAL.name());
            bi.setPrice(BigDecimal.valueOf(r.getAmount()));
            items.add(bi);
        }
        return items;
    }
}
