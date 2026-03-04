package com.dissayakesuper.web_pos_backend.inventory.repository;

import com.dissayakesuper.web_pos_backend.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /** Find the inventory record linked to a specific product. */
    Optional<Inventory> findByProductId(Long productId);

    /** Check whether an inventory record exists for a product. */
    boolean existsByProductId(Long productId);
}
