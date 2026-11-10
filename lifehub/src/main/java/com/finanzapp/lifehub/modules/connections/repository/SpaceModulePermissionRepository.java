package com.finanzapp.lifehub.modules.connections.repository;

import com.finanzapp.lifehub.modules.connections.repository.entities.SpaceModulePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface SpaceModulePermissionRepository extends JpaRepository<SpaceModulePermissionEntity, Long> {
    Optional<SpaceModulePermissionEntity> findBySpaceIdAndModuleKey(Long spaceId, String moduleKey);
    List<SpaceModulePermissionEntity> findBySpaceId(Long spaceId);
}
