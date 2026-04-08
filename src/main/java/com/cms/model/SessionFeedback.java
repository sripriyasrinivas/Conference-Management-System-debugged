package com.cms.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "session_feedbacks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @ToString.Exclude          // CRITICAL FIX
    @EqualsAndHashCode.Exclude // CRITICAL FIX
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendee_id", nullable = false)
    @ToString.Exclude          // CRITICAL FIX
    @EqualsAndHashCode.Exclude // CRITICAL FIX
    private User attendee;

    private int contentRating;
    private int speakerRating;
    private int overallRating;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime submittedAt = LocalDateTime.now();
}