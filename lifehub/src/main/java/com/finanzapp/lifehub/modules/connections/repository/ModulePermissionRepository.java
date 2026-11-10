package com.finanzapp.lifehub.modules.connections.repository;

import com.finanzapp.lifehub.modules.connections.repository.entities.ModulePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModulePermissionRepository extends JpaRepository<ModulePermissionEntity, Long> {
    List<ModulePermissionEntity> findByConnectionId(Long connectionId);
    Optional<ModulePermissionEntity> findByConnectionIdAndModuleKey(Long connectionId, String moduleKey);
}
