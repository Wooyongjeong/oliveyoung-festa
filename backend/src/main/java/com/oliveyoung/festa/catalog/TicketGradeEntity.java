package com.oliveyoung.festa.catalog;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ticket_grades")
public class TicketGradeEntity {
    @Id private UUID id;
    @ManyToOne @JoinColumn(name = "event_id") private EventEntity event;
    private String code;
    private String name;
    private BigDecimal price;
    private String currency;
    protected TicketGradeEntity() {}
    public UUID getId() { return id; }
    public EventEntity getEvent() { return event; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
}
