package com.dissayakesuper.web_pos_backend.reorder.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reorders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reorder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_ref", nullable = false, unique = true, length = 50)
    private String orderRef;

    @Column(name = "supplier_email", nullable = false, length = 255)
    private String supplierEmail;

    @Column(name = "supplier_accept_token", length = 120, unique = true)
    private String supplierAcceptToken;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @OneToMany(
            mappedBy = "reorder",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<ReorderItem> items = new ArrayList<>();

    // ── Convenience helper ────────────────────────────────────────────────────

    public void addItem(ReorderItem item) {
        items.add(item);
        item.setReorder(this);
    }

    public void removeItem(ReorderItem item) {
        items.remove(item);
        item.setReorder(null);
    }
}
