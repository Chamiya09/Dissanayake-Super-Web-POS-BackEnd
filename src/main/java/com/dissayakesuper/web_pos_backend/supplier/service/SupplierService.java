package com.dissayakesuper.web_pos_backend.supplier.service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.dissayakesuper.web_pos_backend.inventory.repository.InventoryRepository;
import com.dissayakesuper.web_pos_backend.product.entity.Product;
import com.dissayakesuper.web_pos_backend.product.entity.ProductStatus;
import com.dissayakesuper.web_pos_backend.product.repository.ProductRepository;
import com.dissayakesuper.web_pos_backend.reorder.entity.Status;
import com.dissayakesuper.web_pos_backend.reorder.repository.ReorderRepository;
import com.dissayakesuper.web_pos_backend.supplier.dto.SupplierRequest;
import com.dissayakesuper.web_pos_backend.supplier.entity.Supplier;
import com.dissayakesuper.web_pos_backend.supplier.repository.SupplierRepository;

@Service
@Transactional
public class SupplierService {

    private static final double ZERO_TOLERANCE = 0.000001d;

    private final SupplierRepository repository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final ReorderRepository reorderRepository;

    public SupplierService(
            SupplierRepository repository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            ReorderRepository reorderRepository) {
        this.repository = repository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.reorderRepository = reorderRepository;
    }

    // ── READ ALL ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Supplier> getAllSuppliers() {
        return repository.findAll();
    }

    // ── READ ONE ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Supplier getSupplierById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Supplier not found with id: " + id));
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    public Supplier createSupplier(SupplierRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A supplier with email '" + request.email() + "' already exists.");
        }

        Supplier supplier = new Supplier(
                request.companyName(),
                request.contactPerson(),
                request.email(),
                request.phone(),
                request.leadTime(),
                request.isAutoReorderEnabled()
        );

        return repository.save(supplier);
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    public Supplier updateSupplier(Long id, SupplierRequest request) {
        Supplier existing = getSupplierById(id);

        // Guard: reject if the new email is already taken by a *different* supplier
        repository.findByEmail(request.email())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Email '" + request.email() + "' is already used by another supplier.");
                });

        existing.setCompanyName(request.companyName());
        existing.setContactPerson(request.contactPerson());
        existing.setEmail(request.email());
        existing.setPhone(request.phone());
        existing.setLeadTime(request.leadTime());
        existing.setAutoReorderEnabled(request.isAutoReorderEnabled());

        return repository.save(existing);
    }

    public Supplier updateSupplierActiveStatus(Long id, boolean active) {
        Supplier supplier = getSupplierById(id);
        if (supplier.isActive() == active) {
            return supplier;
        }

        supplier.setActive(active);

        List<Product> singleSourceProducts = productRepository.findBySupplierIdAndIsActiveTrue(id);
        ProductStatus nextStatus = active ? ProductStatus.ACTIVE : ProductStatus.DISCONTINUED;
        singleSourceProducts.forEach(product -> product.setStatus(nextStatus));
        productRepository.saveAll(singleSourceProducts);

        if (!active && !singleSourceProducts.isEmpty()) {
            List<Long> productIds = singleSourceProducts.stream()
                    .map(Product::getId)
                    .toList();
            reorderRepository.markOrdersForProducts(productIds, Status.PENDING, Status.CANCELLED);
        }

        return repository.save(supplier);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    public void deleteSupplier(Long id) {
        Supplier supplier = getSupplierById(id);

        List<Product> assignedProducts = productRepository.findBySupplierIdAndIsActiveTrue(id);
        if (!assignedProducts.isEmpty()) {
            List<String> nonZeroStockProducts = assignedProducts.stream()
                    .filter(product -> currentInventoryQty(product) > ZERO_TOLERANCE)
                    .map(product -> product.getProductName() + " (stock: " + formatQty(currentInventoryQty(product)) + ")")
                    .collect(Collectors.toList());

            if (!nonZeroStockProducts.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Supplier '" + supplier.getCompanyName() + "' cannot be deleted. All assigned product inventory must be 0 first. "
                                + "Products with remaining stock: " + String.join(", ", nonZeroStockProducts));
            }

            // Business rule: when supplier is removed and all assigned product stock is zero,
            // soft-delete those assigned products and release their barcodes.
            assignedProducts.forEach(product -> {
                product.setSupplier(null);
                product.setSku(null);
                product.setActive(false);
                product.setStatus(ProductStatus.DISCONTINUED);
            });
            productRepository.saveAll(assignedProducts);
        }

        repository.delete(supplier);
    }

    // ── ASSIGN PRODUCTS ───────────────────────────────────────────────────────

    public void assignProducts(Long supplierId, List<Long> productIds) {
        Supplier supplier = getSupplierById(supplierId);
        List<Product> products = productRepository.findAllById(productIds);
        if (products.size() != productIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "One or more product IDs were not found.");
        }
        ProductStatus assignedStatus = supplier.isActive() ? ProductStatus.ACTIVE : ProductStatus.DISCONTINUED;
        products.forEach(p -> {
            p.setSupplier(supplier);
            p.setStatus(assignedStatus);
        });
        productRepository.saveAll(products);

        if (!supplier.isActive()) {
            reorderRepository.markOrdersForProducts(productIds, Status.PENDING, Status.CANCELLED);
        }
    }

    private double currentInventoryQty(Product product) {
        return inventoryRepository.findByProductId(product.getId())
                .map(inv -> {
                    Double stockQty = inv.getStockQuantity();
                    return stockQty != null ? stockQty : 0.0;
                })
                // Source of truth is inventory table. If no inventory row exists, treat as zero.
                .orElse(0.0);
    }

    private String formatQty(double qty) {
        if (Math.abs(qty - Math.rint(qty)) < ZERO_TOLERANCE) {
            return String.format(Locale.ENGLISH, "%.0f", qty);
        }
        return String.format(Locale.ENGLISH, "%.3f", qty);
    }
}
