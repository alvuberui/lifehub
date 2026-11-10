package com.finanzapp.lifehub.modules.connections.repository.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "shared_space_members", schema = "connections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedSpaceMemberEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Column(name = "user_email", nullable = false, length = 320)
    private String userEmail;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;
}
