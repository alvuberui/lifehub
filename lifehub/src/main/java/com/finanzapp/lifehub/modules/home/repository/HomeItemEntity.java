package com.finanzapp.lifehub.modules.home.repository;

import com.finanzapp.lifehub.modules.home.domain.HomeItemStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "items", schema = "home")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HomeItemEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "owner_email", nullable = false)
    private String ownerEmail;
    @Column(nullable = false)
    private boolean shared;
    @Column(nullable = false, length = 180)
    private String name;
    @Column(nullable = false, length = 2000)
    private String description;
    @Column(name = "category_id", nullable = false)
    private Long categoryId;
    @Column(nullable = false, length = 120)
    private String brand;
    @Column(nullable = false, length = 120)
    private String model;
    @Column(name = "serial_number", length = 160)
    private String serialNumber;
    @Column(length = 160)
    private String location;
    @Column(nullable = false, length = 180)
    private String supplier;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32)
    private HomeItemStatus status;
    @Column(precision = 14, scale = 2)
    private BigDecimal price;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;
    @Column(name = "warranty_end_date", nullable = false)
    private LocalDate warrantyEndDate;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "archived_at")
    private Instant archivedAt;
}
