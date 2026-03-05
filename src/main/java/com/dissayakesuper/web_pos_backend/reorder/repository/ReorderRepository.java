package com.dissayakesuper.web_pos_backend.reorder.repository;

import com.dissayakesuper.web_pos_backend.reorder.entity.Reorder;
import com.dissayakesuper.web_pos_backend.reorder.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReorderRepository extends JpaRepository<Reorder, Long> {

    Optional<Reorder> findByOrderRef(String orderRef);

    boolean existsByOrderRef(String orderRef);

    List<Reorder> findAllByStatus(Status status);

    List<Reorder> findAllBySupplierEmail(String supplierEmail);
}
