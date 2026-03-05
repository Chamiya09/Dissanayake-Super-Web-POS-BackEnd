package com.dissayakesuper.web_pos_backend.inventory.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    /** Returns all records where current stock is at or below the reorder threshold.
     *  Uses JOIN FETCH to load the associated Product and its optional Supplier
     *  in a single query (avoids N+1). Supplier is LEFT JOIN because it is nullable. */
    @Query("SELECT i FROM Inventory i JOIN FETCH i.product p LEFT JOIN FETCH p.supplier WHERE i.stockQuantity <= i.reorderLevel ORDER BY i.stockQuantity ASC")
    List<Inventory> findAllLowStock();
}
