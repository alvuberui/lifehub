package com.finanzapp.lifehub.modules.home.repository;

import com.finanzapp.lifehub.modules.home.domain.AttachmentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "attachments", schema = "home")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HomeAttachmentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "item_id", nullable = false)
    private Long itemId;
    @Column(name = "owner_email", nullable = false)
    private String ownerEmail;
    @Enumerated(EnumType.STRING) @Column(name = "document_type", nullable = false, length = 32)
    private AttachmentType documentType;
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;
    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;
    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
