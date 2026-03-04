package com.dissayakesuper.web_pos_backend.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Audit log — one row is written every time stock is manually updated.
 * Intentionally denormalised (stores productName snapshot) so the history
 * remains meaningful even if the product is later renamed or deleted.
 */
@Entity
@Table(name = "inventory_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK reference — kept for JOIN queries, but nullable so logs survive product deletion. */
    @Column(name = "product_id")
    private Long productId;

    /** Snapshot of product name at the time of the stock change. */
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    /** Positive = stock added. Negative = stock removed (future use). */
    @Column(name = "quantity_changed", nullable = false)
    private Double quantityChanged;

    /** Stock level immediately after this update. */
    @Column(name = "stock_after", nullable = false)
    private Double stockAfter;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
}
