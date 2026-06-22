package com.biddy.payment.wallet.infrastructure.client.toss;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TossPaymentClient {

    private final RestClient restClient;
    private final String secretKey;

    public TossPaymentClient(
            RestClient.Builder restClientBuilder,
            @Value("${toss.payments.base-url}") String baseUrl,
            @Value("${toss.payments.secret-key:}") String secretKey
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.secretKey = secretKey;
    }

    public TossPaymentConfirmResponse confirm(String paymentKey, String orderId, Long amount) {
        return restClient.post()
                .uri("/v1/payments/confirm")
                .header(HttpHeaders.AUTHORIZATION, basicAuthorizationHeader())
                .body(new TossPaymentConfirmRequest(paymentKey, orderId, amount))
                .retrieve()
                .body(TossPaymentConfirmResponse.class);
    }

    public TossPaymentCancelResponse cancel(String paymentKey, String cancelReason, Long cancelAmount) {
        return restClient.post()
                .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                .header(HttpHeaders.AUTHORIZATION, basicAuthorizationHeader())
                .body(new TossPaymentCancelRequest(cancelReason, cancelAmount))
                .retrieve()
                .body(TossPaymentCancelResponse.class);
    }

    private String basicAuthorizationHeader() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("Toss Payments secret key is required.");
        }

        String token = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }
}
