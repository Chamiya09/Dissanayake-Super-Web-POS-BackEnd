package com.dissayakesuper.web_pos_backend.sale.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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

    @NotBlank(message = "Payment method is required.")
    @Size(max = 50, message = "Payment method must be 50 characters or fewer.")
    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;

    @NotNull(message = "Total amount is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total amount must be 0 or greater.")
    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @NotBlank(message = "Sale status is required.")
    @Size(max = 20, message = "Sale status must be 20 characters or fewer.")
    @Column(name = "status", nullable = false, length = 20)
    private String status;   // "Completed" | "Voided"

    @Column(name = "cashier_id")
    private Long cashierId;

    @Column(name = "cashier_username", length = 50)
    private String cashierUsername;

    @Column(name = "cashier_name", length = 150)
    private String cashierName;

    @NotEmpty(message = "Sale must include at least one item.")
    @Valid
    @JsonManagedReference
    @OneToMany(
            mappedBy      = "sale",
            cascade       = CascadeType.ALL,
            orphanRemoval = true,
            fetch         = FetchType.EAGER
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

    @JsonProperty("transactionId")
    public String getTransactionId() { return receiptNo; }

    @JsonProperty("transactionId")
    public void setTransactionId(String transactionId) { this.receiptNo = transactionId; }

    public LocalDateTime getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDateTime saleDate) { this.saleDate = saleDate; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getCashierId() { return cashierId; }
    public void setCashierId(Long cashierId) { this.cashierId = cashierId; }

    public String getCashierUsername() { return cashierUsername; }
    public void setCashierUsername(String cashierUsername) { this.cashierUsername = cashierUsername; }

    public String getCashierName() { return cashierName; }
    public void setCashierName(String cashierName) { this.cashierName = cashierName; }

    public List<SaleItem> getItems() { return items; }
    public void setItems(List<SaleItem> items) { this.items = items; }
}
