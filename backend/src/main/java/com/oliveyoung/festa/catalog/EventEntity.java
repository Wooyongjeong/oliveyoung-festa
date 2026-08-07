package com.oliveyoung.festa.catalog;

import com.oliveyoung.festa.persistence.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventEntity extends BaseEntity {
    @Id private UUID id;
    private String name;
    private String description;
    private Instant saleStartsAt;
    private Instant saleEndsAt;
    private Instant eventStartsAt;
}
