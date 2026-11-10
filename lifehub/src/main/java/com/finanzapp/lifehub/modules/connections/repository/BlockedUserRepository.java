package com.finanzapp.lifehub.modules.connections.repository;

import com.finanzapp.lifehub.modules.connections.repository.entities.BlockedUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockedUserRepository extends JpaRepository<BlockedUserEntity, Long> {
    boolean existsByBlockerEmailAndBlockedEmail(String blockerEmail, String blockedEmail);
}
