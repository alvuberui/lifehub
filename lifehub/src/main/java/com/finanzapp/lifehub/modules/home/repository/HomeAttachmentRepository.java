package com.finanzapp.lifehub.modules.home.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HomeAttachmentRepository extends JpaRepository<HomeAttachmentEntity, Long> {
    List<HomeAttachmentEntity> findAllByItemIdAndOwnerEmailOrderByCreatedAtDesc(Long itemId, String ownerEmail);
    Optional<HomeAttachmentEntity> findByIdAndItemIdAndOwnerEmail(Long id, Long itemId, String ownerEmail);
    long countByItemIdAndOwnerEmail(Long itemId, String ownerEmail);
}
