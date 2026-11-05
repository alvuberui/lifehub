package com.finanzapp.tags.repository;


import com.finanzapp.tags.repository.entities.TagEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<TagEntity, Long> {

    boolean existsByNameAndUserEmailAndDeletedFalse(String name, String userEmail);

    List<TagEntity> findByUserEmailAndDeletedFalse(String userEmail);

    Optional<TagEntity> findByIdAndUserEmailAndDeletedFalse(Long id, String userEmail);

    Optional<TagEntity> findByIdAndUserEmail(Long id, String userEmail);

    boolean existsByIdAndUserEmailAndDeletedFalse(Long id, String userEmail);

    List<TagEntity> findByUserEmailAndIdInAndDeletedFalse(String userEmail, List<Long> ids);




}
