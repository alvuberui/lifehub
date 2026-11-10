package com.finanzapp.lifehub.modules.connections.repository;

import com.finanzapp.lifehub.modules.connections.repository.entities.SharedSpaceMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SharedSpaceMemberRepository extends JpaRepository<SharedSpaceMemberEntity, Long> {
    List<SharedSpaceMemberEntity> findBySpaceIdOrderByJoinedAtAsc(Long spaceId);
    Optional<SharedSpaceMemberEntity> findBySpaceIdAndUserEmail(Long spaceId, String userEmail);
}
