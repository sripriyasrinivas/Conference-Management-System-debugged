package com.cms.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "registrations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendee_id", nullable = false)
    private User attendee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conference_id", nullable = false)
    private Conference conference;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RegistrationStatus status = RegistrationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private RegistrationType type;

    private String dietaryPreferences;
    private String specialRequirements;

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime registeredAt = LocalDateTime.now();

    private LocalDateTime confirmedAt;
    
    // Certificate Tracking
    @Builder.Default
    private boolean certificateIssued = false;
    
    @Builder.Default
    private boolean attendanceConfirmed = false;
    
    private LocalDateTime certificateIssuedAt;
    
    private int sessionsAttended;
    
    // Role Assignment (based on payment/workshop selection)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AssignedRole assignedRole = AssignedRole.ATTENDEE;
    
    // Workshop Selection (comma-separated workshop IDs)
    @Column(columnDefinition = "TEXT")
    private String selectedWorkshops;
    
    // Input Validation Tracking
    @Builder.Default
    private boolean formValidated = false;
    
    @Column(columnDefinition = "TEXT")
    private String validationErrors;

    public enum RegistrationStatus {
        PENDING, CONFIRMED, CANCELLED, WAITLISTED
    }

    public enum RegistrationType {
        STUDENT, PROFESSIONAL, ACADEMIC, VIRTUAL
    }
    
    public enum AssignedRole {
        ATTENDEE("Attendee"),
        VIP("VIP - All Workshops"),
        WORKSHOP_ONLY("Workshop Only - Selected");
        
        private final String displayName;
        
        AssignedRole(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
