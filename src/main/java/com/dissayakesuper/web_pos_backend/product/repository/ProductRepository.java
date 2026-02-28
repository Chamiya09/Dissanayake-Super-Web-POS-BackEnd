package com.dissayakesuper.web_pos_backend.product.repository;

import com.dissayakesuper.web_pos_backend.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Check whether a product with the given SKU already exists. */
    boolean existsBySku(String sku);

    /** Find a product by SKU (useful for duplicate-check on update). */
    Optional<Product> findBySku(String sku);
}
