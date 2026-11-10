package com.finanzapp.lifehub.modules.connections.repository.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "blocked_users", schema = "connections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockedUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "blocker_email", nullable = false, length = 320)
    private String blockerEmail;

    @Column(name = "blocked_email", nullable = false, length = 320)
    private String blockedEmail;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
