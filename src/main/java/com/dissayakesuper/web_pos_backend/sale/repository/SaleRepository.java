package com.dissayakesuper.web_pos_backend.sale.repository;

import com.dissayakesuper.web_pos_backend.sale.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    /** Check receipt number uniqueness before saving. */
    boolean existsByReceiptNo(String receiptNo);

    /** Fetch a sale by its human-readable receipt number. */
    Optional<Sale> findByReceiptNo(String receiptNo);
}
