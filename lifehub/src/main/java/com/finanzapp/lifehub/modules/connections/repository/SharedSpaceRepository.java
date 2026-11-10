package com.finanzapp.lifehub.modules.connections.repository;

import com.finanzapp.lifehub.modules.connections.repository.entities.SharedSpaceEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SharedSpaceRepository extends JpaRepository<SharedSpaceEntity, Long> {
    @Query("select distinct s from SharedSpaceEntity s left join SharedSpaceMemberEntity m on m.spaceId = s.id where lower(s.ownerEmail) = lower(:email) or lower(m.userEmail) = lower(:email) order by s.createdAt desc")
    List<SharedSpaceEntity> findVisibleTo(@Param("email") String email);
}
