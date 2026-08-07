package com.oliveyoung.festa.order;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OrderRepository {

    private final JdbcClient jdbcClient;

    public OrderRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<TicketGradeSnapshot> findGradeForSale(UUID eventId, String gradeCode) {
        return jdbcClient.sql("""
                        SELECT e.id AS event_id, e.name AS event_name, tg.id AS ticket_grade_id,
                               tg.code, tg.name AS grade_name, tg.price, tg.currency
                        FROM events e
                        JOIN ticket_grades tg ON tg.event_id = e.id
                        WHERE e.id = :eventId
                          AND tg.code = :gradeCode
                          AND e.sale_starts_at <= CURRENT_TIMESTAMP
                          AND CURRENT_TIMESTAMP < e.sale_ends_at
                        """)
                .param("eventId", eventId)
                .param("gradeCode", gradeCode)
                .query((rs, rowNum) -> new TicketGradeSnapshot(
                        rs.getObject("event_id", UUID.class), rs.getString("event_name"),
                        rs.getObject("ticket_grade_id", UUID.class), rs.getString("code"),
                        rs.getString("grade_name"), rs.getBigDecimal("price"), rs.getString("currency")))
                .optional();
    }

    public Optional<StoredIdempotentOrder> findByIdempotencyKey(UUID userId, String idempotencyKey) {
        return jdbcClient.sql("SELECT id, request_hash FROM orders WHERE user_id = :userId AND idempotency_key = :key")
                .param("userId", userId)
                .param("key", idempotencyKey)
                .query((rs, rowNum) -> new StoredIdempotentOrder(rs.getObject("id", UUID.class), rs.getString("request_hash")))
                .optional();
    }

    public void insertOrder(UUID orderId, UUID userId, TicketGradeSnapshot grade, String idempotencyKey, String requestHash) {
        jdbcClient.sql("""
                        INSERT INTO orders (id, user_id, event_id, ticket_grade_id, status, idempotency_key, request_hash,
                                            unit_price, currency, grade_name_snapshot, event_name_snapshot)
                        VALUES (:id, :userId, :eventId, :ticketGradeId, 'HELD', :key, :requestHash,
                                :price, :currency, :gradeName, :eventName)
                        """)
                .param("id", orderId).param("userId", userId).param("eventId", grade.eventId())
                .param("ticketGradeId", grade.ticketGradeId()).param("key", idempotencyKey).param("requestHash", requestHash)
                .param("price", grade.price()).param("currency", grade.currency()).param("gradeName", grade.gradeName())
                .param("eventName", grade.eventName()).update();
    }

    public void ensurePurchaseRight(UUID userId, UUID eventId) {
        jdbcClient.sql("""
                        INSERT INTO purchase_rights (user_id, event_id, status)
                        VALUES (:userId, :eventId, 'AVAILABLE')
                        ON CONFLICT (user_id, event_id) DO NOTHING
                        """).param("userId", userId).param("eventId", eventId).update();
    }

    public boolean claimPurchaseRight(UUID userId, UUID eventId, UUID orderId) {
        return jdbcClient.sql("""
                        UPDATE purchase_rights
                        SET status = 'HELD', order_id = :orderId, updated_at = CURRENT_TIMESTAMP
                        WHERE user_id = :userId AND event_id = :eventId AND status = 'AVAILABLE'
                        """).param("userId", userId).param("eventId", eventId).param("orderId", orderId).update() == 1;
    }

    public boolean holdInventory(UUID ticketGradeId) {
        return jdbcClient.sql("""
                        UPDATE inventories
                        SET available = available - 1, held = held + 1, version = version + 1
                        WHERE ticket_grade_id = :ticketGradeId AND available > 0
                        """).param("ticketGradeId", ticketGradeId).update() == 1;
    }

    public void insertReservation(UUID reservationId, UUID orderId) {
        jdbcClient.sql("""
                        INSERT INTO reservations (id, order_id, status, expires_at)
                        VALUES (:id, :orderId, 'ACTIVE', CURRENT_TIMESTAMP + INTERVAL '3 minutes')
                        """).param("id", reservationId).param("orderId", orderId).update();
    }

    public void insertHistory(UUID historyId, UUID orderId) {
        jdbcClient.sql("""
                        INSERT INTO order_status_histories (id, order_id, from_status, to_status, reason)
                        VALUES (:id, :orderId, NULL, 'HELD', 'ORDER_CREATED')
                        """).param("id", historyId).param("orderId", orderId).update();
    }

    public Optional<OrderView> findOrder(UUID orderId) {
        return jdbcClient.sql(orderViewQuery("WHERE o.id = :orderId"))
                .param("orderId", orderId).query((rs, rowNum) -> mapOrder(rs)).optional();
    }

    public List<OrderView> findOrdersByUser(UUID userId) {
        return jdbcClient.sql(orderViewQuery("WHERE o.user_id = :userId ORDER BY o.created_at DESC"))
                .param("userId", userId).query((rs, rowNum) -> mapOrder(rs)).list();
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

    private OrderView mapOrder(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new OrderView(rs.getObject("id", UUID.class), rs.getObject("event_id", UUID.class),
                rs.getString("event_name_snapshot"), rs.getString("code"), rs.getString("grade_name_snapshot"),
                rs.getBigDecimal("unit_price"), rs.getString("currency"), OrderStatus.valueOf(rs.getString("status")),
                rs.getObject("expires_at", java.time.OffsetDateTime.class).toInstant(),
                rs.getObject("created_at", java.time.OffsetDateTime.class).toInstant());
    }

    public record TicketGradeSnapshot(UUID eventId, String eventName, UUID ticketGradeId, String gradeCode,
                                      String gradeName, BigDecimal price, String currency) {
    }

    public record StoredIdempotentOrder(UUID id, String requestHash) {
    }
}
