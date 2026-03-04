package com.dissayakesuper.web_pos_backend.inventory.repository;

import com.dissayakesuper.web_pos_backend.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /** Find the inventory record linked to a specific product. */
    Optional<Inventory> findByProductId(Long productId);

    /** Check whether an inventory record exists for a product. */
    boolean existsByProductId(Long productId);

    /** Returns all records where current stock is below the reorder threshold. */
    @Query("SELECT i FROM Inventory i WHERE i.stockQuantity < i.reorderLevel")
    List<Inventory> findAllLowStock();
}
