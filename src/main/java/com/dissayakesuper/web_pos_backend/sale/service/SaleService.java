package com.dissayakesuper.web_pos_backend.sale.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.dissayakesuper.web_pos_backend.inventory.entity.Inventory;
import com.dissayakesuper.web_pos_backend.inventory.entity.InventoryLog;
import com.dissayakesuper.web_pos_backend.inventory.repository.InventoryLogRepository;
import com.dissayakesuper.web_pos_backend.inventory.repository.InventoryRepository;
import com.dissayakesuper.web_pos_backend.sale.entity.Sale;
import com.dissayakesuper.web_pos_backend.sale.entity.SaleItem;
import com.dissayakesuper.web_pos_backend.sale.repository.SaleRepository;

@Service
@Transactional
public class SaleService {

    private final SaleRepository        saleRepository;
    private final InventoryRepository   inventoryRepository;
    private final InventoryLogRepository inventoryLogRepository;

    public SaleService(SaleRepository saleRepository,
                       InventoryRepository inventoryRepository,
                       InventoryLogRepository inventoryLogRepository) {
        this.saleRepository       = saleRepository;
        this.inventoryRepository  = inventoryRepository;
        this.inventoryLogRepository = inventoryLogRepository;
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    public Sale createSale(Sale sale) {
        if (saleRepository.existsByReceiptNo(sale.getReceiptNo())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A sale with receipt number '" + sale.getReceiptNo() + "' already exists.");
        }

        // ── Deduct stock for every item in this sale ──────────────────────────
        for (SaleItem item : sale.getItems()) {
            item.setSale(sale);

            // Look up inventory by product_id (preferred) or fall back to product name
            Inventory inventory = (item.getProductId() != null
                    ? inventoryRepository.findByProductId(item.getProductId())
                    : inventoryRepository.findByProductProductName(item.getProductName()))
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "No inventory record found for product: '" + item.getProductName() + "'"));

            double soldQty = item.getQuantity().doubleValue();
            if (inventory.getStockQuantity() < soldQty) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Insufficient stock for '" + item.getProductName() +
                        "'. Available: " + inventory.getStockQuantity() +
                        ", requested: " + soldQty);
            }

            double newQty = inventory.getStockQuantity() - soldQty;
            inventory.setStockQuantity(newQty);
            inventoryRepository.save(inventory);

            // ── Audit log (negative quantityChanged = stock removed) ──────────
            inventoryLogRepository.save(InventoryLog.builder()
                    .productId(inventory.getProduct().getId())
                    .productName(inventory.getProduct().getProductName())
                    .quantityChanged(-soldQty)
                    .stockAfter(newQty)
                    .build());
        }

        return saleRepository.save(sale);
    }

    // ── READ ALL ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Sale> getAllSales() {
        return saleRepository.findAll();
    }

    // ── READ ONE ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Sale getSaleById(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sale not found with id: " + id));
    }

    // ── VOID / UPDATE STATUS ──────────────────────────────────────────────────

    public Sale updateSaleStatus(Long id, String newStatus) {
        Sale sale = getSaleById(id);

        if ("Voided".equalsIgnoreCase(sale.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Sale " + sale.getReceiptNo() + " is already voided and cannot be modified.");
        }

        sale.setStatus(newStatus);
        return saleRepository.save(sale);
    }
}
