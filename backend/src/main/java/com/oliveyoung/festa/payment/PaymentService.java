package com.oliveyoung.festa.payment;

import com.oliveyoung.festa.api.ApiStatus;
import com.oliveyoung.festa.auth.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository repository;
    private final PaymentGateway gateway;
    private final TransactionTemplate transactions;

    public PaymentService(PaymentRepository repository, PaymentGateway gateway, TransactionTemplate transactions) {
        this.repository = repository;
        this.gateway = gateway;
        this.transactions = transactions;
    }

    public PaymentView pay(AuthenticatedUser user, UUID orderId, String idempotencyKey, String scenario) {
        Prepared prepared = transactions.execute(status -> prepare(user, orderId, idempotencyKey));
        if (prepared.existing() != null) return prepared.existing();

        try {
            var result = gateway.approve(prepared.attemptKey(), prepared.order().amount(), prepared.order().currency(), scenario);
            return transactions.execute(status -> apply(prepared.order(), result));
        } catch (ResourceAccessException exception) {
            return transactions.execute(status -> {
                repository.markUnknown(prepared.attemptKey(), "PG_RESPONSE_TIMEOUT");
                return repository.findActive(orderId).orElseThrow();
            });
        }
    }

    public PaymentView reconcile(AuthenticatedUser user, UUID orderId) {
        Prepared prepared = transactions.execute(status -> {
            repository.lockOrder(orderId, user.id()).orElseThrow(() -> new PaymentException(ApiStatus.PAYMENT_NOT_AVAILABLE));
            PaymentView active = repository.findActive(orderId)
                    .orElseThrow(() -> new PaymentException(ApiStatus.PAYMENT_NOT_RECONCILABLE));
            if (active.status() == PaymentStatus.PROCESSING) throw new PaymentException(ApiStatus.PAYMENT_NOT_RECONCILABLE);
            return new Prepared(repository.lockOrder(orderId, user.id()).orElseThrow(), active.attemptKey(), active);
        });
        var result = gateway.find(prepared.attemptKey());
        if (result.status() == PaymentGateway.Status.NOT_FOUND) return prepared.existing();
        return transactions.execute(status -> apply(prepared.order(), result));
    }

    @Scheduled(fixedDelay = 5_000, initialDelay = 5_000)
    public void reconcileUnknownPayments() {
        var unknownPayments = transactions.execute(status -> repository.findUnknownForReconciliation());
        for (PaymentView payment : unknownPayments) {
            try {
                var result = gateway.find(payment.attemptKey());
                if (result.status() != PaymentGateway.Status.NOT_FOUND) {
                    transactions.executeWithoutResult(status -> {
                        var order = repository.lockOrder(payment.orderId()).orElseThrow();
                        apply(order, result);
                    });
                } else if (payment.status() == PaymentStatus.UNKNOWN
                        && Duration.between(payment.updatedAt(), Instant.now()).compareTo(Duration.ofSeconds(60)) >= 0) {
                    transactions.executeWithoutResult(status ->
                            repository.markReviewRequired(payment.attemptKey(), "PG_RESULT_NOT_FOUND"));
                }
            } catch (RuntimeException ignored) {
                // 다음 주기에 다시 조회한다.
            }
        }
    }

    private Prepared prepare(AuthenticatedUser user, UUID orderId, String idempotencyKey) {
        var order = repository.lockOrder(orderId, user.id())
                .orElseThrow(() -> new PaymentException(ApiStatus.PAYMENT_NOT_AVAILABLE));
        var repeated = repository.findByIdempotencyKey(orderId, idempotencyKey);
        if (repeated.isPresent()) return new Prepared(order, repeated.get().attemptKey(), repeated.get());
        if (!"HELD".equals(order.status()) || repository.findActive(orderId).isPresent()) {
            throw new PaymentException(ApiStatus.PAYMENT_NOT_AVAILABLE);
        }
        long failures = repository.declinedCount(orderId);
        if (failures >= 3) throw new PaymentException(ApiStatus.PAYMENT_RETRY_EXHAUSTED);
        Instant lastAttempt = repository.lastAttemptAt(orderId);
        if (lastAttempt != null && Duration.between(lastAttempt, Instant.now()).compareTo(Duration.ofSeconds(2)) < 0) {
            throw new PaymentException(ApiStatus.PAYMENT_RETRY_TOO_SOON);
        }
        String attemptKey = UUID.randomUUID().toString();
        repository.start(orderId, attemptKey, idempotencyKey, order.amount(), order.currency());
        return new Prepared(order, attemptKey, null);
    }

    private PaymentView apply(PaymentRepository.PaymentOrder order, PaymentGateway.PaymentResult result) {
        PaymentView current = repository.findActive(order.id()).orElseGet(() ->
                repository.findByAttemptKey(order.id(), result.attemptKey()).orElseThrow());
        if (current.status() == PaymentStatus.APPROVED || current.status() == PaymentStatus.DECLINED) return current;
        if (result.status() == PaymentGateway.Status.APPROVED) {
            if (order.amount().compareTo(result.amount()) != 0 || !order.currency().equals(result.currency())) {
                repository.markReviewRequired(result.attemptKey(), "AMOUNT_OR_CURRENCY_MISMATCH");
            } else {
                repository.approve(order, result.attemptKey(), result.transactionId());
            }
        } else if (result.status() == PaymentGateway.Status.DECLINED) {
            repository.decline(order, result.attemptKey(), repository.declinedCount(order.id()) + 1 >= 3);
        }
        return repository.findByAttemptKey(order.id(), current.attemptKey()).orElseThrow();
    }

    private record Prepared(PaymentRepository.PaymentOrder order, String attemptKey, PaymentView existing) {}
}
