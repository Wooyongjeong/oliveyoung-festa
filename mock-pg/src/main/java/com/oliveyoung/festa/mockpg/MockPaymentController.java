package com.oliveyoung.festa.mockpg;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/mock-api")
public class MockPaymentController {
    private final JdbcClient jdbc;

    public MockPaymentController(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping("/payments")
    public PaymentResponse approve(@RequestBody PaymentRequest request,
                                   @RequestHeader(value = "X-Mock-Scenario", defaultValue = "SUCCESS") String scenario)
            throws InterruptedException {
        var existing = find(request.attemptKey());
        if (existing.isPresent()) return existing.get();

        String status = "DECLINE".equals(scenario) ? "DECLINED" : "APPROVED";
        String transactionId = "APPROVED".equals(status) ? UUID.randomUUID().toString() : null;
        jdbc.sql("""
                INSERT INTO mock_payments (attempt_key, amount, currency, status, transaction_id)
                VALUES (:key, :amount, :currency, :status, :transactionId)
                """).param("key", request.attemptKey()).param("amount", request.amount())
                .param("currency", request.currency()).param("status", status)
                .param("transactionId", transactionId).update();

        if ("TIMEOUT_AFTER_SUCCESS".equals(scenario)) Thread.sleep(3_000);
        return find(request.attemptKey()).orElseThrow();
    }

    @GetMapping("/payments/{attemptKey}")
    public PaymentResponse findPayment(@PathVariable String attemptKey) {
        return find(attemptKey).orElseThrow(PaymentNotFoundException::new);
    }

    @PostMapping("/refunds")
    public RefundResponse refund(@RequestBody RefundRequest request) {
        jdbc.sql("""
                MERGE INTO mock_refunds (refund_key, attempt_key, status) KEY (refund_key)
                VALUES (:refundKey, :attemptKey, 'REFUNDED')
                """).param("refundKey", request.refundKey()).param("attemptKey", request.attemptKey()).update();
        return new RefundResponse(request.refundKey(), request.attemptKey(), "REFUNDED");
    }

    private Optional<PaymentResponse> find(String attemptKey) {
        return jdbc.sql("""
                SELECT attempt_key, amount, currency, status, transaction_id
                FROM mock_payments WHERE attempt_key = :key
                """).param("key", attemptKey).query((rs, row) -> new PaymentResponse(
                rs.getString("attempt_key"), rs.getBigDecimal("amount"), rs.getString("currency"),
                rs.getString("status"), rs.getString("transaction_id"))).optional();
    }

    public record PaymentRequest(String attemptKey, BigDecimal amount, String currency) {}
    public record PaymentResponse(String attemptKey, BigDecimal amount, String currency, String status, String transactionId) {}
    public record RefundRequest(String refundKey, String attemptKey) {}
    public record RefundResponse(String refundKey, String attemptKey, String status) {}

    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class PaymentNotFoundException extends RuntimeException {}
}
