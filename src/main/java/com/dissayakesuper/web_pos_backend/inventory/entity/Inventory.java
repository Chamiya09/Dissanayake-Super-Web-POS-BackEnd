package com.dissayakesuper.web_pos_backend.inventory.entity;

import com.dissayakesuper.web_pos_backend.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private Double stockQuantity = 0.0;

    @Column(name = "reorder_level", nullable = false)
    @Builder.Default
    private Double reorderLevel = 10.0;

    @Column(name = "unit", length = 20)
    private String unit;    // kg, g, l, ml, pcs

    @UpdateTimestamp
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
}
