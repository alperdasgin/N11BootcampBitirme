package com.ecommerce.payment_service.service;

import com.ecommerce.payment_service.dto.*;
import com.ecommerce.payment_service.entity.Payment;
import com.ecommerce.payment_service.repository.PaymentRepository;
import com.iyzipay.Options;
import com.iyzipay.model.*;
import com.iyzipay.request.CreatePaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Value("${iyzico.api-key}") private String apiKey;
    @Value("${iyzico.secret-key}") private String secretKey;
    @Value("${iyzico.base-url}") private String baseUrl;

    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Ödeme işlemi başladı. orderId={}, amount={}", request.getOrderId(), request.getAmount());

        try {
            Options options = new Options();
            options.setApiKey(apiKey);
            options.setSecretKey(secretKey);
            options.setBaseUrl(baseUrl);

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

            // Kart bilgileri
            PaymentCard paymentCard = new PaymentCard();
            paymentCard.setCardHolderName(request.getCard().getCardHolderName());
            paymentCard.setCardNumber(request.getCard().getCardNumber());
            paymentCard.setExpireMonth(request.getCard().getExpireMonth());
            paymentCard.setExpireYear(request.getCard().getExpireYear());
            paymentCard.setCvc(request.getCard().getCvc());
            paymentCard.setRegisterCard(0);
            paymentRequest.setPaymentCard(paymentCard);

            // Alıcı bilgileri
            Buyer buyer = new Buyer();
            buyer.setId("USER-" + request.getUsername());
            buyer.setName(request.getFirstName());
            buyer.setSurname(request.getLastName());
            buyer.setEmail(request.getEmail());
            buyer.setIdentityNumber("74300864791");
            buyer.setRegistrationAddress(request.getAddress());
            buyer.setCity(request.getCity());
            buyer.setCountry(request.getCountry());
            buyer.setIp("127.0.0.1");
            paymentRequest.setBuyer(buyer);

            // Adres
            Address address = new Address();
            address.setContactName(request.getFirstName() + " " + request.getLastName());
            address.setCity(request.getCity());
            address.setCountry(request.getCountry());
            address.setAddress(request.getAddress());
            paymentRequest.setShippingAddress(address);
            paymentRequest.setBillingAddress(address);

            // Ürünler
            List<BasketItem> basketItems = new ArrayList<>();
            if (request.getItems() != null) {
                for (PaymentRequest.Item item : request.getItems()) {
                    BasketItem basketItem = new BasketItem();
                    basketItem.setId("PRODUCT-" + item.getProductId());
                    basketItem.setName(item.getProductName());
                    basketItem.setCategory1("Genel");
                    basketItem.setItemType(BasketItemType.PHYSICAL.name());
                    basketItem.setPrice(BigDecimal.valueOf(item.getPrice() * item.getQuantity()));
                    basketItems.add(basketItem);
                }
            } else {
                BasketItem basketItem = new BasketItem();
                basketItem.setId("ORDER-" + request.getOrderId());
                basketItem.setName("Sipariş");
                basketItem.setCategory1("Genel");
                basketItem.setItemType(BasketItemType.PHYSICAL.name());
                basketItem.setPrice(BigDecimal.valueOf(request.getAmount()));
                basketItems.add(basketItem);
            }
            paymentRequest.setBasketItems(basketItems);

            // Iyzico'ya gönder
            com.iyzipay.model.Payment iyzicoPayment =
                    com.iyzipay.model.Payment.create(paymentRequest, options);

            boolean success = "success".equalsIgnoreCase(iyzicoPayment.getStatus());

            // DB'ye kaydet
            Payment payment = Payment.builder()
                    .orderId(request.getOrderId())
                    .username(request.getUsername())
                    .amount(request.getAmount())
                    .status(success ? "SUCCESS" : "FAILED")
                    .transactionId(iyzicoPayment.getPaymentId())
                    .errorMessage(success ? null : iyzicoPayment.getErrorMessage())
                    .build();
            paymentRepository.save(payment);

            log.info("Ödeme sonucu: success={}, paymentId={}", success, iyzicoPayment.getPaymentId());

            return PaymentResponse.builder()
                    .success(success)
                    .transactionId(iyzicoPayment.getPaymentId())
                    .message(success ? "Ödeme başarılı" : iyzicoPayment.getErrorMessage())
                    .orderId(request.getOrderId())
                    .build();

        } catch (Exception e) {
            log.error("Ödeme hatası: {}", e.getMessage());
            Payment payment = Payment.builder()
                    .orderId(request.getOrderId())
                    .username(request.getUsername())
                    .amount(request.getAmount())
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .build();
            paymentRepository.save(payment);

            return PaymentResponse.builder()
                    .success(false)
                    .message("Ödeme hatası: " + e.getMessage())
                    .orderId(request.getOrderId())
                    .build();
        }
    }

    public List<Payment> getPaymentsByOrder(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }
}