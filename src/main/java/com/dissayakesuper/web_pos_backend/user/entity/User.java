package com.dissayakesuper.web_pos_backend.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username is required.")
    @Size(max = 50, message = "Username must be 50 characters or fewer.")
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank(message = "Member ID is required.")
    @Size(max = 30, message = "Member ID must be 30 characters or fewer.")
    @Column(name = "member_id", unique = true, length = 30)
    private String memberId;

    @NotBlank(message = "Full name is required.")
    @Size(max = 150, message = "Full name must be 150 characters or fewer.")
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 150, message = "Email must be 150 characters or fewer.")
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Size(max = 30, message = "Phone number must be 30 characters or fewer.")
    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Size(max = 255, message = "Address must be 255 characters or fewer.")
    @Column(name = "address", length = 255)
    private String address;

    @NotBlank(message = "Password hash is required.")
    @Size(max = 255, message = "Password hash must be 255 characters or fewer.")
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @NotBlank(message = "Role is required.")
    @Size(max = 30, message = "Role must be 30 characters or fewer.")
    @Column(nullable = false, length = 30)
    private String role;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "email_notifications", nullable = false)
    private boolean emailNotifications = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
