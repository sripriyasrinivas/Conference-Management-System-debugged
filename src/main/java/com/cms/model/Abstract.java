package com.cms.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "abstracts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Abstract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String keywords;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    @ToString.Exclude             // CRITICAL: Prevents infinite loop with User
    @EqualsAndHashCode.Exclude    // CRITICAL: Prevents infinite loop in hash calculations
    private User author;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AbstractStatus status = AbstractStatus.SUBMITTED;

    private String reviewComments;

    @Builder.Default              // Ensures version starts at 1 when using Builder
    private int version = 1;

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime submittedAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum AbstractStatus {
        SUBMITTED, APPROVED, REJECTED, REVISION_REQUESTED
    }
}