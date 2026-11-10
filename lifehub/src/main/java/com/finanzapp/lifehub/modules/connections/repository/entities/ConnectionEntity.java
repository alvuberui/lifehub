package com.finanzapp.lifehub.modules.connections.repository.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "connections", schema = "connections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_a_email", nullable = false, length = 320)
    private String userAEmail;

    @Column(name = "user_b_email", nullable = false, length = 320)
    private String userBEmail;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
