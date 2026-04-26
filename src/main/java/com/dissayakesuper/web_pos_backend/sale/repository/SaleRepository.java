package com.dissayakesuper.web_pos_backend.sale.repository;

import com.dissayakesuper.web_pos_backend.sale.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    /** Check receipt number uniqueness before saving. */
    boolean existsByReceiptNo(String receiptNo);

    /** Fetch a sale by its human-readable receipt number. */
    Optional<Sale> findByReceiptNo(String receiptNo);

    /** Fetch the lexicographically highest TRX id (safe because ids are zero-padded). */
    Optional<Sale> findFirstByReceiptNoStartingWithOrderByReceiptNoDesc(String prefix);

    /** Export helper ordered by sale date and id. */
    java.util.List<Sale> findAllByOrderBySaleDateAscIdAsc();

    List<Sale> findAllByOrderBySaleDateDescIdDesc();

    List<Sale> findByCashierIdOrderBySaleDateDescIdDesc(Long cashierId);

        @Query("""
                        SELECT COALESCE(SUM(s.totalAmount), 0)
                        FROM Sale s
                        WHERE s.status = 'Completed'
                            AND s.saleDate >= :startTime
                            AND s.saleDate <= :endTime
                        """)
        Double sumCompletedSalesBetween(@Param("startTime") LocalDateTime startTime,
                                                                        @Param("endTime") LocalDateTime endTime);
}
