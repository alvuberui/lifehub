package com.finanzapp.lifehub.modules.home.repository;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "categories", schema = "home")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HomeCategoryEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "owner_email", nullable = false)
    private String ownerEmail;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(length = 32)
    private String color;
    @Column(length = 64)
    private String icon;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
