package com.finanzapp.lifehub.modules.home.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface HomeCategoryRepository extends JpaRepository<HomeCategoryEntity, Long> {
    List<HomeCategoryEntity> findAllByOwnerEmailOrderByNameAsc(String ownerEmail);
    List<HomeCategoryEntity> findAllByOwnerEmailInOrderByNameAsc(Collection<String> ownerEmails);
    Optional<HomeCategoryEntity> findByIdAndOwnerEmail(Long id, String ownerEmail);
    boolean existsByOwnerEmailAndNameIgnoreCase(String ownerEmail, String name);
}
