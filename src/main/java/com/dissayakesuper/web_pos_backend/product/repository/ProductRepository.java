package com.dissayakesuper.web_pos_backend.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.dissayakesuper.web_pos_backend.product.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Check whether a product with the given SKU already exists. */
    boolean existsBySku(String sku);

    /** Find a product by SKU (useful for duplicate-check on update). */
    Optional<Product> findBySku(String sku);

    /** Find all products assigned to a specific supplier. */
    List<Product> findBySupplierId(Long supplierId);

    /** Find all products not yet linked to any supplier. */
    List<Product> findBySupplierIsNull();

    /** Returns products that do not yet have an Inventory record. */
    @Query("SELECT p FROM Product p WHERE p.id NOT IN (SELECT i.product.id FROM Inventory i)")
    List<Product> findProductsNotInInventory();
}
