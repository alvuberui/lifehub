package com.finanzapp.lifehub.modules.identity.reporting.repository.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "monthly_report_dispatches",
        schema = "identity",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_monthly_report_user_month",
                columnNames = {"user_email", "report_month"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class MonthlyReportDispatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    @Column(name = "report_month", nullable = false)
    private LocalDate reportMonth;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
