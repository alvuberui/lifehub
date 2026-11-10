package com.finanzapp.lifehub.modules.connections.repository.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "shared_spaces", schema = "connections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedSpaceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "owner_email", nullable = false, length = 320)
    private String ownerEmail;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
