package com.oliveyoung.festa.payment;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PaymentRepository {
    @PersistenceContext private EntityManager entityManager;

    public Optional<PaymentOrder> lockOrder(UUID orderId, UUID userId) {
        @SuppressWarnings("unchecked") List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT id, status, unit_price, currency, ticket_grade_id
                FROM orders WHERE id = :orderId AND user_id = :userId FOR UPDATE
                """).setParameter("orderId", orderId).setParameter("userId", userId).getResultList();
        return rows.stream().findFirst().map(r -> new PaymentOrder((UUID) r[0], (String) r[1],
                (BigDecimal) r[2], (String) r[3], (UUID) r[4]));
    }

    public Optional<PaymentOrder> lockOrder(UUID orderId) {
        @SuppressWarnings("unchecked") List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT id, status, unit_price, currency, ticket_grade_id
                FROM orders WHERE id = :orderId FOR UPDATE
                """).setParameter("orderId", orderId).getResultList();
        return rows.stream().findFirst().map(r -> new PaymentOrder((UUID) r[0], (String) r[1],
                (BigDecimal) r[2], (String) r[3], (UUID) r[4]));
    }

    public List<PaymentView> findUnknownForReconciliation() {
        @SuppressWarnings("unchecked") List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT order_id, attempt_key, status, amount, currency, failure_reason, updated_at
                FROM payment_attempts
                WHERE (status = 'UNKNOWN' AND updated_at <= CURRENT_TIMESTAMP - INTERVAL '5 seconds')
                   OR (status = 'REVIEW_REQUIRED' AND updated_at <= CURRENT_TIMESTAMP - INTERVAL '1 minute')
                ORDER BY updated_at LIMIT 20
                """).getResultList();
        return rows.stream().map(this::map).toList();
    }

    public Optional<PaymentView> findByIdempotencyKey(UUID orderId, String key) {
        return find("WHERE order_id = :orderId AND idempotency_key = :key", orderId, key);
    }

    public Optional<PaymentView> findActive(UUID orderId) {
        return find("WHERE order_id = :orderId AND status IN ('PROCESSING','UNKNOWN','REVIEW_REQUIRED')", orderId, null);
    }

    public Optional<PaymentView> findByAttemptKey(UUID orderId, String attemptKey) {
        return find("WHERE order_id = :orderId AND attempt_key = :key", orderId, attemptKey);
    }

    private Optional<PaymentView> find(String where, UUID orderId, String key) {
        var query = entityManager.createNativeQuery("""
                SELECT order_id, attempt_key, status, amount, currency, failure_reason, updated_at
                FROM payment_attempts
                """ + where).setParameter("orderId", orderId);
        if (key != null) query.setParameter("key", key);
        @SuppressWarnings("unchecked") List<Object[]> rows = query.getResultList();
        return rows.stream().findFirst().map(this::map);
    }

    public long declinedCount(UUID orderId) {
        return ((Number) entityManager.createNativeQuery("SELECT count(*) FROM payment_attempts WHERE order_id=:id AND status='DECLINED'")
                .setParameter("id", orderId).getSingleResult()).longValue();
    }

    public Instant lastAttemptAt(UUID orderId) {
        Object value = entityManager.createNativeQuery("SELECT max(created_at) FROM payment_attempts WHERE order_id=:id")
                .setParameter("id", orderId).getSingleResult();
        return value == null ? null : instant(value);
    }

    public void start(UUID orderId, String attemptKey, String idempotencyKey, BigDecimal amount, String currency) {
        entityManager.createNativeQuery("""
                INSERT INTO payment_attempts (id, order_id, attempt_key, idempotency_key, pg_provider, status, amount, currency)
                VALUES (:id, :orderId, :attemptKey, :key, 'MOCK_PG', 'PROCESSING', :amount, :currency)
                """).setParameter("id", UUID.randomUUID()).setParameter("orderId", orderId)
                .setParameter("attemptKey", attemptKey).setParameter("key", idempotencyKey)
                .setParameter("amount", amount).setParameter("currency", currency).executeUpdate();
        entityManager.createNativeQuery("UPDATE orders SET status='PAYMENT_PROCESSING', updated_at=CURRENT_TIMESTAMP WHERE id=:id AND status='HELD'")
                .setParameter("id", orderId).executeUpdate();
    }

    public void markUnknown(String attemptKey, String reason) {
        updateAttempt(attemptKey, "UNKNOWN", null, reason);
    }

    public void markReviewRequired(String attemptKey, String reason) {
        updateAttempt(attemptKey, "REVIEW_REQUIRED", null, reason);
    }

    public void approve(PaymentOrder order, String attemptKey, String transactionId) {
        updateAttempt(attemptKey, "APPROVED", transactionId, null);
        entityManager.createNativeQuery("UPDATE orders SET status='PAID', updated_at=CURRENT_TIMESTAMP WHERE id=:id AND status='PAYMENT_PROCESSING'")
                .setParameter("id", order.id()).executeUpdate();
        entityManager.createNativeQuery("UPDATE reservations SET status='RELEASED' WHERE order_id=:id AND status='ACTIVE'")
                .setParameter("id", order.id()).executeUpdate();
        entityManager.createNativeQuery("UPDATE inventories SET held=held-1, sold=sold+1, version=version+1 WHERE ticket_grade_id=:gradeId AND held>0")
                .setParameter("gradeId", order.ticketGradeId()).executeUpdate();
        entityManager.createNativeQuery("UPDATE purchase_rights SET status='PURCHASED', updated_at=CURRENT_TIMESTAMP WHERE order_id=:id AND status='HELD'")
                .setParameter("id", order.id()).executeUpdate();
        history(order.id(), "PAYMENT_PROCESSING", "PAID", "PAYMENT_APPROVED");
        entityManager.createNativeQuery("""
                INSERT INTO outbox_events (id, aggregate_type, aggregate_id, event_type, payload)
                VALUES (:eventId, 'ORDER', :orderId, 'OrderPaid', CAST(:payload AS TEXT))
                """).setParameter("eventId", UUID.randomUUID()).setParameter("orderId", order.id())
                .setParameter("payload", "{\"orderId\":\"" + order.id() + "\"}").executeUpdate();
    }

    public void decline(PaymentOrder order, String attemptKey, boolean finalFailure) {
        updateAttempt(attemptKey, "DECLINED", null, "DECLINED_BY_PG");
        String next = finalFailure ? "PAYMENT_FAILED" : "HELD";
        entityManager.createNativeQuery("UPDATE orders SET status=:status, updated_at=CURRENT_TIMESTAMP WHERE id=:id AND status='PAYMENT_PROCESSING'")
                .setParameter("status", next).setParameter("id", order.id()).executeUpdate();
        history(order.id(), "PAYMENT_PROCESSING", next, "PAYMENT_DECLINED");
        if (finalFailure) {
            entityManager.createNativeQuery("UPDATE reservations SET status='RELEASED' WHERE order_id=:id AND status='ACTIVE'")
                    .setParameter("id", order.id()).executeUpdate();
            entityManager.createNativeQuery("UPDATE inventories SET available=available+1, held=held-1, version=version+1 WHERE ticket_grade_id=:gradeId AND held>0")
                    .setParameter("gradeId", order.ticketGradeId()).executeUpdate();
            entityManager.createNativeQuery("UPDATE purchase_rights SET status='AVAILABLE', order_id=NULL, updated_at=CURRENT_TIMESTAMP WHERE order_id=:id")
                    .setParameter("id", order.id()).executeUpdate();
        }
    }

    private void updateAttempt(String attemptKey, String status, String transactionId, String reason) {
        entityManager.createNativeQuery("""
                UPDATE payment_attempts SET status=:status, pg_transaction_id=:transactionId,
                    failure_reason=:reason, updated_at=CURRENT_TIMESTAMP WHERE attempt_key=:attemptKey
                """).setParameter("status", status).setParameter("transactionId", transactionId)
                .setParameter("reason", reason).setParameter("attemptKey", attemptKey).executeUpdate();
    }

    private void history(UUID orderId, String from, String to, String reason) {
        entityManager.createNativeQuery("""
                INSERT INTO order_status_histories (id, order_id, from_status, to_status, reason)
                VALUES (:id, :orderId, :from, :to, :reason)
                """).setParameter("id", UUID.randomUUID()).setParameter("orderId", orderId)
                .setParameter("from", from).setParameter("to", to).setParameter("reason", reason).executeUpdate();
    }

    private PaymentView map(Object[] r) {
        return new PaymentView((UUID) r[0], (String) r[1], PaymentStatus.valueOf((String) r[2]),
                (BigDecimal) r[3], (String) r[4], (String) r[5], instant(r[6]));
    }

    private Instant instant(Object value) {
        return value instanceof Instant i ? i : ((OffsetDateTime) value).toInstant();
    }

    public record PaymentOrder(UUID id, String status, BigDecimal amount, String currency, UUID ticketGradeId) {}
}
