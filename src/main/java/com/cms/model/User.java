package com.cms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true)
    private String username;

    @NotBlank
    @Email
    @Column(unique = true)
    private String email;

    @NotBlank
    private String password;

    private String fullName;
    private String institution;
    private String bio;
    private String phone;

    @Enumerated(EnumType.STRING)
    private Role role;

    // FIX: Exclude collections from ToString to prevent infinite recursion
    @OneToMany(mappedBy = "author")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Paper> papers;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Role {
        ADMIN, AUTHOR, REVIEWER, SPEAKER, ATTENDEE
    }
}