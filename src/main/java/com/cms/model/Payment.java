package com.cms.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendee_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User attendee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conference_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Conference conference;

    @Enumerated(EnumType.STRING)
    private PackageType packageType;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    private String transactionId;

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime completedAt;

    public enum PackageType {
        BASIC("Basic Registration", BigDecimal.valueOf(0)),
        VIP("VIP Package (All workshops + Materials)", BigDecimal.valueOf(50.00)),
        WORKSHOP_ONLY("Selected Workshops Only", BigDecimal.valueOf(25.00));

        private final String displayName;
        private final BigDecimal price;

        PackageType(String displayName, BigDecimal price) {
            this.displayName = displayName;
            this.price = price;
        }

        public String getDisplayName() {
            return displayName;
        }

        public BigDecimal getPrice() {
            return price;
        }
    }

    public enum PaymentStatus {
        PENDING, COMPLETED, FAILED, REFUNDED
    }
}
