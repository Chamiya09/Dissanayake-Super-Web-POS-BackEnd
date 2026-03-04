package com.dissayakesuper.web_pos_backend.inventory.repository;

import com.dissayakesuper.web_pos_backend.inventory.entity.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {

    /** All log entries for a specific product, newest first. */
    List<InventoryLog> findByProductIdOrderByTimestampDesc(Long productId);

    /** All log entries across all products, newest first. */
    List<InventoryLog> findAllByOrderByTimestampDesc();
}
