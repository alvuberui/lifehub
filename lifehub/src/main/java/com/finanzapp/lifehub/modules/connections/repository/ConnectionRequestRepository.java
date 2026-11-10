package com.finanzapp.lifehub.modules.connections.repository;

import com.finanzapp.lifehub.modules.connections.domain.ConnectionRequestStatus;
import com.finanzapp.lifehub.modules.connections.repository.entities.ConnectionRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConnectionRequestRepository extends JpaRepository<ConnectionRequestEntity, Long> {
    List<ConnectionRequestEntity> findByRequesterEmailOrRecipientEmailOrderByCreatedAtDesc(String requesterEmail, String recipientEmail);
    Optional<ConnectionRequestEntity> findFirstByRequesterEmailAndRecipientEmailAndStatusOrderByCreatedAtDesc(String requesterEmail, String recipientEmail, ConnectionRequestStatus status);
}
