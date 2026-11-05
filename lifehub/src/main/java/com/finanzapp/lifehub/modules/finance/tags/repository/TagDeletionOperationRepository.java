package com.finanzapp.tags.repository;

import com.finanzapp.tags.repository.entities.TagDeletionOperationEntity;
import com.finanzapp.tags.repository.entities.TagDeletionState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TagDeletionOperationRepository extends JpaRepository<TagDeletionOperationEntity, Long> {

    Optional<TagDeletionOperationEntity> findByTagIdAndUserEmail(Long tagId, String userEmail);

    List<TagDeletionOperationEntity> findTop20ByStateAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            TagDeletionState state,
            Instant now
    );
}
