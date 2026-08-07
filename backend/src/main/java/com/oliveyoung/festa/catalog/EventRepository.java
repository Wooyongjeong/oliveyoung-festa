package com.oliveyoung.festa.catalog;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EventRepository {

    private final JdbcClient jdbcClient;

    public EventRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<EventSummary> findAll() {
        return jdbcClient.sql("""
                        SELECT id, name, sale_starts_at, sale_ends_at, event_starts_at, CURRENT_TIMESTAMP AS database_now
                        FROM events
                        ORDER BY event_starts_at, id
                        """)
                .query(this::mapSummary)
                .list();
    }

    public Optional<EventDetail> findById(UUID eventId) {
        Optional<EventRow> event = jdbcClient.sql("""
                        SELECT id, name, description, sale_starts_at, sale_ends_at, event_starts_at,
                               CURRENT_TIMESTAMP AS database_now
                        FROM events
                        WHERE id = :eventId
                        """)
                .param("eventId", eventId)
                .query((rs, rowNum) -> new EventRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getObject("sale_starts_at", java.time.OffsetDateTime.class).toInstant(),
                        rs.getObject("sale_ends_at", java.time.OffsetDateTime.class).toInstant(),
                        rs.getObject("event_starts_at", java.time.OffsetDateTime.class).toInstant(),
                        rs.getObject("database_now", java.time.OffsetDateTime.class).toInstant()))
                .optional();

        return event.map(row -> new EventDetail(
                row.id(), row.name(), row.description(), row.saleStartsAt(), row.saleEndsAt(), row.eventStartsAt(),
                EventSaleStatus.at(row.databaseNow(), row.saleStartsAt(), row.saleEndsAt()), findGrades(row.id())));
    }

    private List<EventDetail.TicketGrade> findGrades(UUID eventId) {
        return jdbcClient.sql("""
                        SELECT tg.id, tg.code, tg.name, tg.price, tg.currency, i.total, i.available
                        FROM ticket_grades tg
                        JOIN inventories i ON i.ticket_grade_id = tg.id
                        WHERE tg.event_id = :eventId
                        ORDER BY tg.price, tg.code
                        """)
                .param("eventId", eventId)
                .query((rs, rowNum) -> new EventDetail.TicketGrade(
                        rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name"),
                        rs.getBigDecimal("price"), rs.getString("currency"), rs.getInt("total"),
                        rs.getInt("available")))
                .list();
    }

    private EventSummary mapSummary(ResultSet rs, int rowNum) throws SQLException {
        Instant startsAt = rs.getObject("sale_starts_at", java.time.OffsetDateTime.class).toInstant();
        Instant endsAt = rs.getObject("sale_ends_at", java.time.OffsetDateTime.class).toInstant();
        Instant now = rs.getObject("database_now", java.time.OffsetDateTime.class).toInstant();
        return new EventSummary(
                rs.getObject("id", UUID.class), rs.getString("name"), startsAt, endsAt,
                rs.getObject("event_starts_at", java.time.OffsetDateTime.class).toInstant(),
                EventSaleStatus.at(now, startsAt, endsAt));
    }

    private record EventRow(
            UUID id,
            String name,
            String description,
            Instant saleStartsAt,
            Instant saleEndsAt,
            Instant eventStartsAt,
            Instant databaseNow
    ) {
    }
}
