package com.dissayakesuper.web_pos_backend.product.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.dissayakesuper.web_pos_backend.supplier.entity.Supplier;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Size(max = 100)
    @Column(name = "sku", unique = true, length = 100)
    private String sku;

    @Size(max = 100)
    @Column(name = "barcode", length = 100)
    private String barcode;

    @NotBlank
    @Size(max = 100)
    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Digits(integer = 8, fraction = 2)
    @Column(name = "buying_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal buyingPrice;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Digits(integer = 8, fraction = 2)
    @Column(name = "selling_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal sellingPrice;

    @Size(max = 50)
    @Column(name = "unit", length = 50)
    private String unit;            // e.g. kg, grams, liters, pieces, bottles

    @Column(name = "stock_quantity", nullable = false, columnDefinition = "FLOAT DEFAULT 0.0")
    private Double stockQuantity = 0.0;

    @Column(name = "reorder_level")
    private Double reorderLevel;    // Alert threshold — null means no alert

    // Expose the raw FK as a plain Long so Jackson serializes it without touching the lazy proxy
    @Column(name = "supplier_id", insertable = false, updatable = false)
    private Long supplierId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;      // nullable — not every product has a supplier assigned

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProductStatus status = ProductStatus.ACTIVE;

    // ── Constructors ──────────────────────────────────────────────────────────

    protected Product() {}

    public Product(String productName,
                   String sku,
                   String barcode,
                   String category,
                   BigDecimal buyingPrice,
                   BigDecimal sellingPrice,
                   String unit) {
        this(productName, sku, barcode, category, buyingPrice, sellingPrice, unit, 0.0, null);
    }

    public Product(String productName,
                   String sku,
                   String barcode,
                   String category,
                   BigDecimal buyingPrice,
                   BigDecimal sellingPrice,
                   String unit,
                   Double stockQuantity,
                   Double reorderLevel) {
        this.productName   = productName;
        this.sku           = sku;
        this.barcode       = barcode;
        this.category      = category;
        this.buyingPrice   = buyingPrice;
        this.sellingPrice  = sellingPrice;
        this.unit          = unit;
        this.stockQuantity = stockQuantity != null ? stockQuantity : 0.0;
        this.reorderLevel  = reorderLevel;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getBuyingPrice() { return buyingPrice; }
    public void setBuyingPrice(BigDecimal buyingPrice) { this.buyingPrice = buyingPrice; }

    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Double getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Double stockQuantity) { this.stockQuantity = stockQuantity != null ? stockQuantity : 0.0; }

    public Double getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(Double reorderLevel) { this.reorderLevel = reorderLevel; }

    public Long getSupplierId() { return supplierId; }

    @JsonIgnore
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status != null ? status : ProductStatus.ACTIVE; }
}
