package com.dissayakesuper.web_pos_backend.inventory.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dissayakesuper.web_pos_backend.inventory.entity.Inventory;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /** Find the inventory record linked to a specific product. */
    Optional<Inventory> findByProductId(Long productId);

    /** Find the inventory record by the product's name (used during sale processing). */
    Optional<Inventory> findByProductProductName(String productName);

    /** Check whether an inventory record exists for a product. */
    boolean existsByProductId(Long productId);

    /**
     * Loads an inventory record with its Product eagerly via JOIN FETCH.
     * Use this whenever the caller needs product fields after the transaction ends.
     */
    @Query("SELECT i FROM Inventory i JOIN FETCH i.product p LEFT JOIN FETCH p.supplier WHERE i.id = :id")
    Optional<Inventory> findByIdWithProduct(@Param("id") Long id);

    /** Returns all records where current stock is at or below the reorder threshold.
     *  Uses JOIN FETCH to load the associated Product and its optional Supplier
     *  in a single query (avoids N+1). Supplier is LEFT JOIN because it is nullable. */
    @Query("SELECT i FROM Inventory i JOIN FETCH i.product p LEFT JOIN FETCH p.supplier WHERE p.isActive = true AND p.status <> com.dissayakesuper.web_pos_backend.product.entity.ProductStatus.DISCONTINUED AND i.stockQuantity <= i.reorderLevel ORDER BY i.stockQuantity ASC")
    List<Inventory> findAllLowStock();
}
