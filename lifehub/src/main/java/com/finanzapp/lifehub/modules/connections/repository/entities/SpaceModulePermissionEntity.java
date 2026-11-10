package com.finanzapp.lifehub.modules.connections.repository.entities;

import com.finanzapp.lifehub.modules.connections.domain.ModulePermission;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "space_module_permissions", schema = "connections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpaceModulePermissionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Column(name = "module_key", nullable = false, length = 40)
    private String moduleKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ModulePermission permission;

    @Column(name = "granted_by_email", nullable = false, length = 320)
    private String grantedByEmail;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
