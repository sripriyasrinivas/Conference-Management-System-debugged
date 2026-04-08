package com.cms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "papers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Paper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    @Column(columnDefinition = "TEXT")
    @NotBlank
    private String abstractText;

    @Column(columnDefinition = "TEXT")
    private String keywords;

    private String filePath;
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.SUBMITTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    @ToString.Exclude            // CRITICAL: Prevents infinite loop with User
    @EqualsAndHashCode.Exclude   // CRITICAL: Prevents infinite loop in Sets/Maps
    private User author;

    @OneToMany(mappedBy = "paper", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude            // CRITICAL: Prevents infinite loop with Review
    private List<Review> reviews = new ArrayList<>();

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime submittedAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    private String revisionComments;

    @Builder.Default
    private int version = 1;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum Status {
        SUBMITTED("Submitted"),
        UNDER_REVIEW("Under Review"),
        ACCEPTED("Accepted"),
        REJECTED("Rejected"),
        REVISION_REQUIRED("Revision Required"),
        REVISED("Revised");

        private final String displayName;

        Status(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}