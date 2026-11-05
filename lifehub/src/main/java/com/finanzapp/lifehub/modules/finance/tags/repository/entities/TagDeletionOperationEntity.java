package com.finanzapp.tags.repository.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "tag_deletion_operations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tag_deletion_operation_tag_user",
                columnNames = {"tag_id", "user_email"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagDeletionOperationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 32)
    private TagDeletionState state;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    public static TagDeletionOperationEntity pending(Long tagId, String userEmail) {
        return TagDeletionOperationEntity.builder()
                .tagId(tagId)
                .userEmail(userEmail)
                .state(TagDeletionState.PENDING)
                .attempts(0)
                .nextAttemptAt(Instant.now())
                .build();
    }
}
