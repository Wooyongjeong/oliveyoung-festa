package com.oliveyoung.festa.order;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OrderRepository {

    @PersistenceContext private EntityManager entityManager;

    public Optional<TicketGradeSnapshot> findGradeForSale(UUID eventId, String gradeCode) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT e.id AS event_id, e.name AS event_name, tg.id AS ticket_grade_id,
                               tg.code, tg.name AS grade_name, tg.price, tg.currency
                        FROM events e
                        JOIN ticket_grades tg ON tg.event_id = e.id
                        WHERE e.id = :eventId
                          AND tg.code = :gradeCode
                          AND e.sale_starts_at <= CURRENT_TIMESTAMP
                          AND CURRENT_TIMESTAMP < e.sale_ends_at
                        """).setParameter("eventId", eventId).setParameter("gradeCode", gradeCode).getResultList();
        return rows.stream().findFirst().map(row -> new TicketGradeSnapshot((UUID) row[0], (String) row[1],
                (UUID) row[2], (String) row[3], (String) row[4], (BigDecimal) row[5], (String) row[6]));
    }

    public Optional<StoredIdempotentOrder> findByIdempotencyKey(UUID userId, String idempotencyKey) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("SELECT id, request_hash FROM orders WHERE user_id = :userId AND idempotency_key = :key")
                .setParameter("userId", userId).setParameter("key", idempotencyKey).getResultList();
        return rows.stream().findFirst().map(row -> new StoredIdempotentOrder((UUID) row[0], (String) row[1]));
    }

    public void insertOrder(UUID orderId, UUID userId, TicketGradeSnapshot grade, String idempotencyKey, String requestHash) {
        entityManager.createNativeQuery("""
                        INSERT INTO orders (id, user_id, event_id, ticket_grade_id, status, idempotency_key, request_hash,
                                            unit_price, currency, grade_name_snapshot, event_name_snapshot)
                        VALUES (:id, :userId, :eventId, :ticketGradeId, 'HELD', :key, :requestHash,
                                :price, :currency, :gradeName, :eventName)
                        """).setParameter("id", orderId).setParameter("userId", userId).setParameter("eventId", grade.eventId())
                .setParameter("ticketGradeId", grade.ticketGradeId()).setParameter("key", idempotencyKey).setParameter("requestHash", requestHash)
                .setParameter("price", grade.price()).setParameter("currency", grade.currency()).setParameter("gradeName", grade.gradeName())
                .setParameter("eventName", grade.eventName()).executeUpdate();
    }

    public void ensurePurchaseRight(UUID userId, UUID eventId) {
        entityManager.createNativeQuery("""
                        INSERT INTO purchase_rights (user_id, event_id, status)
                        VALUES (:userId, :eventId, 'AVAILABLE')
                        ON CONFLICT (user_id, event_id) DO NOTHING
                        """).setParameter("userId", userId).setParameter("eventId", eventId).executeUpdate();
    }

    public boolean claimPurchaseRight(UUID userId, UUID eventId, UUID orderId) {
        return entityManager.createNativeQuery("""
                        UPDATE purchase_rights
                        SET status = 'HELD', order_id = :orderId, updated_at = CURRENT_TIMESTAMP
                        WHERE user_id = :userId AND event_id = :eventId AND status = 'AVAILABLE'
                        """).setParameter("userId", userId).setParameter("eventId", eventId).setParameter("orderId", orderId).executeUpdate() == 1;
    }

    public boolean holdInventory(UUID ticketGradeId) {
        return entityManager.createNativeQuery("""
                        UPDATE inventories
                        SET available = available - 1, held = held + 1, version = version + 1
                        WHERE ticket_grade_id = :ticketGradeId AND available > 0
                        """).setParameter("ticketGradeId", ticketGradeId).executeUpdate() == 1;
    }

    public void insertReservation(UUID reservationId, UUID orderId) {
        entityManager.createNativeQuery("""
                        INSERT INTO reservations (id, order_id, status, expires_at)
                        VALUES (:id, :orderId, 'ACTIVE', CURRENT_TIMESTAMP + INTERVAL '3 minutes')
                        """).setParameter("id", reservationId).setParameter("orderId", orderId).executeUpdate();
    }

    public void insertHistory(UUID historyId, UUID orderId) {
        entityManager.createNativeQuery("""
                        INSERT INTO order_status_histories (id, order_id, from_status, to_status, reason)
                        VALUES (:id, :orderId, NULL, 'HELD', 'ORDER_CREATED')
                        """).setParameter("id", historyId).setParameter("orderId", orderId).executeUpdate();
    }

    public Optional<OrderView> findOrder(UUID orderId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(orderViewQuery("WHERE o.id = :orderId"))
                .setParameter("orderId", orderId).getResultList();
        return rows.stream().findFirst().map(this::mapOrder);
    }

    public List<OrderView> findOrdersByUser(UUID userId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(orderViewQuery("WHERE o.user_id = :userId ORDER BY o.created_at DESC"))
                .setParameter("userId", userId).getResultList();
        return rows.stream().map(this::mapOrder).toList();
    }

    private String orderViewQuery(String whereClause) {
        return """
                SELECT o.id, o.event_id, o.event_name_snapshot, tg.code, o.grade_name_snapshot, o.unit_price,
                       o.currency, o.status, r.expires_at, o.created_at
                FROM orders o
                JOIN ticket_grades tg ON tg.id = o.ticket_grade_id
                LEFT JOIN reservations r ON r.order_id = o.id
                """ + whereClause;
    }

    private OrderView mapOrder(Object[] row) {
        return new OrderView((UUID) row[0], (UUID) row[1], (String) row[2], (String) row[3], (String) row[4],
                (BigDecimal) row[5], (String) row[6], OrderStatus.valueOf((String) row[7]),
                toInstant(row[8]), toInstant(row[9]));
    }

    private java.time.Instant toInstant(Object value) {
        if (value instanceof java.time.Instant instant) return instant;
        return ((java.time.OffsetDateTime) value).toInstant();
    }

    public record TicketGradeSnapshot(UUID eventId, String eventName, UUID ticketGradeId, String gradeCode,
                                      String gradeName, BigDecimal price, String currency) {
    }

    public record StoredIdempotentOrder(UUID id, String requestHash) {
    }
}
