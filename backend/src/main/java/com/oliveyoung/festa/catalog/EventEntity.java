package com.oliveyoung.festa.catalog;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
public class EventEntity {
    @Id private UUID id;
    private String name;
    private String description;
    private Instant saleStartsAt;
    private Instant saleEndsAt;
    private Instant eventStartsAt;
    protected EventEntity() {}
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Instant getSaleStartsAt() { return saleStartsAt; }
    public Instant getSaleEndsAt() { return saleEndsAt; }
    public Instant getEventStartsAt() { return eventStartsAt; }
}
