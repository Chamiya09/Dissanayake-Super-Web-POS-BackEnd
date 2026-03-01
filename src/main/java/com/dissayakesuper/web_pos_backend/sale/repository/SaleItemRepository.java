package com.dissayakesuper.web_pos_backend.sale.repository;

import com.dissayakesuper.web_pos_backend.sale.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    /** Retrieve all line items belonging to a specific sale. */
    List<SaleItem> findBySaleId(Long saleId);
}
