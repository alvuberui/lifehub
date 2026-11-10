package com.finanzapp.lifehub.modules.connections.repository.entities;

import com.finanzapp.lifehub.modules.connections.domain.ModulePermission;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "module_permissions", schema = "connections", uniqueConstraints = @UniqueConstraint(name = "module_permissions_unique_module", columnNames = {"connection_id", "module_key"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModulePermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "connection_id", nullable = false)
    private Long connectionId;

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
