package com.dissayakesuper.web_pos_backend.product.service;

import com.dissayakesuper.web_pos_backend.product.dto.ProductRequest;
import com.dissayakesuper.web_pos_backend.product.entity.Product;
import com.dissayakesuper.web_pos_backend.product.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    // ── READ ALL ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return repository.findAll();
    }

    // ── READ ONE ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found with id: " + id));
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    public Product createProduct(ProductRequest request) {
        if (repository.existsBySku(request.sku())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A product with SKU '" + request.sku() + "' already exists.");
        }

        Product product = new Product(
                request.productName(),
                request.sku(),
                request.category(),
                request.buyingPrice(),
                request.sellingPrice(),
                request.unit()
        );

        return repository.save(product);
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    public Product updateProduct(Long id, ProductRequest request) {
        Product existing = getProductById(id);

        // Guard: reject if the new SKU is already taken by a *different* product
        repository.findBySku(request.sku())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "SKU '" + request.sku() + "' is already used by another product.");
                });

        existing.setProductName(request.productName());
        existing.setSku(request.sku());
        existing.setCategory(request.category());
        existing.setBuyingPrice(request.buyingPrice());
        existing.setSellingPrice(request.sellingPrice());
        existing.setUnit(request.unit());

        return repository.save(existing);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    public void deleteProduct(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Product not found with id: " + id);
        }
        repository.deleteById(id);
    }
}
