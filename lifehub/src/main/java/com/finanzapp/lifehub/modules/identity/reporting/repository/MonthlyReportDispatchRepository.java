package com.finanzapp.lifehub.modules.identity.reporting.repository;

import com.finanzapp.lifehub.modules.identity.reporting.repository.entities.MonthlyReportDispatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Instant;

@Repository
public interface MonthlyReportDispatchRepository extends JpaRepository<MonthlyReportDispatchEntity, Long> {

    @Modifying
    @Transactional(transactionManager = "identityTransactionManager")
    @Query(value = """
            INSERT INTO identity.monthly_report_dispatches
                (user_email, report_month, status, last_attempt_at, created_at, updated_at)
            VALUES (:userEmail, :reportMonth, 'PROCESSING', :attemptedAt, :attemptedAt, :attemptedAt)
            ON CONFLICT (user_email, report_month) DO UPDATE
                SET status = 'PROCESSING',
                    last_attempt_at = EXCLUDED.last_attempt_at,
                    updated_at = EXCLUDED.updated_at,
                    error_message = NULL
                WHERE monthly_report_dispatches.status <> 'SENT'
                  AND (monthly_report_dispatches.last_attempt_at IS NULL
                       OR monthly_report_dispatches.last_attempt_at < (:attemptedAt - INTERVAL '15 minutes'))
            """, nativeQuery = true)
    int claimForProcessing(
            @Param("userEmail") String userEmail,
            @Param("reportMonth") LocalDate reportMonth,
            @Param("attemptedAt") Instant attemptedAt
    );

    @Modifying
    @Transactional(transactionManager = "identityTransactionManager")
    @Query(value = """
            UPDATE identity.monthly_report_dispatches
               SET status = 'SENT',
                   sent_at = :sentAt,
                   updated_at = :sentAt,
                   provider_message_id = :providerMessageId,
                   error_message = NULL
             WHERE user_email = :userEmail AND report_month = :reportMonth
            """, nativeQuery = true)
    int markSent(
            @Param("userEmail") String userEmail,
            @Param("reportMonth") LocalDate reportMonth,
            @Param("sentAt") Instant sentAt,
            @Param("providerMessageId") String providerMessageId
    );

    @Modifying
    @Transactional(transactionManager = "identityTransactionManager")
    @Query(value = """
            UPDATE identity.monthly_report_dispatches
               SET status = 'FAILED',
                   updated_at = :updatedAt,
                   error_message = :errorMessage
             WHERE user_email = :userEmail AND report_month = :reportMonth
            """, nativeQuery = true)
    int markFailed(
            @Param("userEmail") String userEmail,
            @Param("reportMonth") LocalDate reportMonth,
            @Param("updatedAt") Instant updatedAt,
            @Param("errorMessage") String errorMessage
    );
}
