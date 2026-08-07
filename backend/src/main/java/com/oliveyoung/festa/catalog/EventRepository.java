package com.oliveyoung.festa.catalog;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EventRepository {

    @PersistenceContext private EntityManager entityManager;

    public List<EventSummary> findAll() {
        return entityManager.createQuery("SELECT e FROM EventEntity e ORDER BY e.eventStartsAt, e.id", EventEntity.class)
                .getResultList().stream().map(this::toSummary).toList();
    }

    public Optional<EventDetail> findById(UUID eventId) {
        EventEntity event = entityManager.find(EventEntity.class, eventId);
        if (event == null) return Optional.empty();
        Instant now = databaseNow();
        return Optional.of(new EventDetail(event.getId(), event.getName(), event.getDescription(), event.getSaleStartsAt(),
                event.getSaleEndsAt(), event.getEventStartsAt(), EventSaleStatus.at(now, event.getSaleStartsAt(), event.getSaleEndsAt()),
                findGrades(eventId)));
    }

    private List<EventDetail.TicketGrade> findGrades(UUID eventId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT tg.id, tg.code, tg.name, tg.price, tg.currency, i.total, i.available
                FROM ticket_grades tg JOIN inventories i ON i.ticket_grade_id = tg.id
                WHERE tg.event_id = :eventId ORDER BY tg.price, tg.code
                """).setParameter("eventId", eventId).getResultList();
        return rows.stream().map(row -> new EventDetail.TicketGrade((UUID) row[0], (String) row[1], (String) row[2],
                (java.math.BigDecimal) row[3], (String) row[4], ((Number) row[5]).intValue(), ((Number) row[6]).intValue())).toList();
    }

    private EventSummary toSummary(EventEntity event) {
        Instant now = databaseNow();
        return new EventSummary(event.getId(), event.getName(), event.getSaleStartsAt(), event.getSaleEndsAt(),
                event.getEventStartsAt(), EventSaleStatus.at(now, event.getSaleStartsAt(), event.getSaleEndsAt()));
    }

    private Instant databaseNow() { return ((java.time.OffsetDateTime) entityManager.createNativeQuery("SELECT CURRENT_TIMESTAMP").getSingleResult()).toInstant(); }
}
