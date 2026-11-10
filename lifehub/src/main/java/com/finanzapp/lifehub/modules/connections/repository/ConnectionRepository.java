package com.finanzapp.lifehub.modules.connections.repository;

import com.finanzapp.lifehub.modules.connections.repository.entities.ConnectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConnectionRepository extends JpaRepository<ConnectionEntity, Long> {
    List<ConnectionEntity> findByUserAEmailOrUserBEmailOrderByCreatedAtDesc(String userAEmail, String userBEmail);
    Optional<ConnectionEntity> findByUserAEmailAndUserBEmail(String userAEmail, String userBEmail);
}
