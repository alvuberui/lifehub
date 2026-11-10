package com.finanzapp.movements.repository.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "forecast_events", schema = "movements")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ForecastEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "event_month", nullable = false)
    private LocalDate eventMonth;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private ForecastEventType eventType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
