package com.dissayakesuper.web_pos_backend.supplier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    /** Check whether a supplier with the given email already exists. */
    boolean existsByEmail(String email);

    /** Find a supplier by email (useful for duplicate-check on update). */
    Optional<Supplier> findByEmail(String email);
}
