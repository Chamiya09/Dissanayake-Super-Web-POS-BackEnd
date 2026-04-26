package com.dissayakesuper.web_pos_backend.reorder.repository;

import com.dissayakesuper.web_pos_backend.reorder.entity.Reorder;
import com.dissayakesuper.web_pos_backend.reorder.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReorderRepository extends JpaRepository<Reorder, Long> {

    Optional<Reorder> findByOrderRef(String orderRef);

    boolean existsByOrderRef(String orderRef);

    List<Reorder> findAllByStatus(Status status);

    List<Reorder> findAllBySupplierEmail(String supplierEmail);

    Optional<Reorder> findBySupplierAcceptToken(String supplierAcceptToken);

    /** Returns all orders sorted newest-first — used by the history endpoint. */
    List<Reorder> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("""
            UPDATE Reorder r
            SET r.status = :nextStatus, r.supplierAcceptToken = null
            WHERE r.status = :currentStatus
              AND EXISTS (
                SELECT i.id FROM ReorderItem i
                WHERE i.reorder = r AND i.productId IN :productIds
              )
            """)
    int markOrdersForProducts(
            @Param("productIds") List<Long> productIds,
            @Param("currentStatus") Status currentStatus,
            @Param("nextStatus") Status nextStatus);
}
