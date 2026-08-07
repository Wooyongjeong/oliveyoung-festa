package com.oliveyoung.festa.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class MockPgPaymentGateway implements PaymentGateway {
    private final RestClient restClient;

    public MockPgPaymentGateway(@Value("${festa.mock-pg.base-url}") String baseUrl,
                                @Value("${festa.mock-pg.connect-timeout}") Duration connectTimeout,
                                @Value("${festa.mock-pg.read-timeout}") Duration readTimeout) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(readTimeout);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public PaymentResult approve(String attemptKey, BigDecimal amount, String currency, String scenario) {
        return restClient.post().uri("/mock-api/payments")
                .header("X-Mock-Scenario", scenario == null ? "SUCCESS" : scenario)
                .body(new ApprovalRequest(attemptKey, amount, currency))
                .retrieve().body(PaymentResult.class);
    }

    @Override
    public PaymentResult find(String attemptKey) {
        try {
            return restClient.get().uri("/mock-api/payments/{attemptKey}", attemptKey)
                    .retrieve().body(PaymentResult.class);
        } catch (HttpClientErrorException.NotFound exception) {
            return new PaymentResult(attemptKey, null, null, Status.NOT_FOUND, null);
        }
    }

    private record ApprovalRequest(String attemptKey, BigDecimal amount, String currency) {}
}
