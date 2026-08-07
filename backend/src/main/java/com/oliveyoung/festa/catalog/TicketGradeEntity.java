package com.oliveyoung.festa.catalog;

import com.oliveyoung.festa.persistence.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ticket_grades")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketGradeEntity extends BaseEntity {
    @Id private UUID id;
    @ManyToOne @JoinColumn(name = "event_id") private EventEntity event;
    private String code;
    private String name;
    private BigDecimal price;
    private String currency;
}
