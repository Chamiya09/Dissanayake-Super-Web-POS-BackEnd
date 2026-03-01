package com.dissayakesuper.web_pos_backend.sale.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_no", nullable = false, unique = true, length = 20)
    private String receiptNo;

    @CreationTimestamp
    @Column(name = "sale_date", nullable = false, updatable = false)
    private LocalDateTime saleDate;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "status", nullable = false, length = 20)
    private String status;   // "Completed" | "Voided"

    @OneToMany(
            mappedBy      = "sale",
            cascade       = CascadeType.ALL,
            orphanRemoval = true,
            fetch         = FetchType.LAZY
    )
    private List<SaleItem> items = new ArrayList<>();

    // ── Constructors ──────────────────────────────────────────────────────────

    public Sale() {}

    public Sale(String receiptNo, String paymentMethod, Double totalAmount, String status) {
        this.receiptNo     = receiptNo;
        this.paymentMethod = paymentMethod;
        this.totalAmount   = totalAmount;
        this.status        = status;
    }

    // ── Convenience helpers ───────────────────────────────────────────────────

    public void addItem(SaleItem item) {
        item.setSale(this);
        this.items.add(item);
    }

    public void removeItem(SaleItem item) {
        item.setSale(null);
        this.items.remove(item);
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReceiptNo() { return receiptNo; }
    public void setReceiptNo(String receiptNo) { this.receiptNo = receiptNo; }

    public LocalDateTime getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDateTime saleDate) { this.saleDate = saleDate; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<SaleItem> getItems() { return items; }
    public void setItems(List<SaleItem> items) { this.items = items; }
}
