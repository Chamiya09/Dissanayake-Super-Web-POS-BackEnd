package com.dissayakesuper.web_pos_backend.supplier.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "suppliers")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @NotBlank
    @Size(max = 255)
    @Column(name = "contact_person", nullable = false, length = 255)
    private String contactPerson;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @NotBlank
    @Size(max = 20)
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Min(1)
    @Column(name = "lead_time", nullable = false)
    private Integer leadTime;

    @Column(name = "is_auto_reorder_enabled", nullable = false)
    private boolean isAutoReorderEnabled = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    protected Supplier() {}

    public Supplier(String companyName,
                    String contactPerson,
                    String email,
                    String phone,
                    Integer leadTime,
                    boolean isAutoReorderEnabled) {
        this.companyName          = companyName;
        this.contactPerson        = contactPerson;
        this.email                = email;
        this.phone                = phone;
        this.leadTime             = leadTime;
        this.isAutoReorderEnabled = isAutoReorderEnabled;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Integer getLeadTime() { return leadTime; }
    public void setLeadTime(Integer leadTime) { this.leadTime = leadTime; }

    public boolean isAutoReorderEnabled() { return isAutoReorderEnabled; }
    public void setAutoReorderEnabled(boolean autoReorderEnabled) { this.isAutoReorderEnabled = autoReorderEnabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
