package com.dissayakesuper.web_pos_backend.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dissayakesuper.web_pos_backend.product.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Check whether a product with the given SKU already exists. */
    boolean existsBySkuAndIsActiveTrue(String sku);

    /** Find a product by SKU (useful for duplicate-check on update). */
    Optional<Product> findBySkuAndIsActiveTrue(String sku);

    Optional<Product> findByIdAndIsActiveTrue(Long id);

    List<Product> findByIsActiveTrue();

    /** Find all products assigned to a specific supplier. */
    List<Product> findBySupplierIdAndIsActiveTrue(Long supplierId);

    /** Find all products not yet linked to any supplier. */
    List<Product> findBySupplierIsNullAndIsActiveTrue();

    /** Returns products that do not yet have an Inventory record. */
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.id NOT IN (SELECT i.product.id FROM Inventory i)")
    List<Product> findProductsNotInInventory();

        Page<Product> findByIsActiveTrue(Pageable pageable);

        @Query(
                        value = """
                                        SELECT p
                                        FROM Product p
                                        WHERE p.isActive = true
                                            AND (
                                                        LOWER(p.productName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                                                        OR LOWER(COALESCE(p.sku, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                                                        OR LOWER(p.category) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                                                    )
                                        """,
                        countQuery = """
                                        SELECT COUNT(p)
                                        FROM Product p
                                        WHERE p.isActive = true
                                            AND (
                                                        LOWER(p.productName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                                                        OR LOWER(COALESCE(p.sku, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                                                        OR LOWER(p.category) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                                                    )
                                        """
        )
        Page<Product> searchActiveProducts(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Modifying
    @Query("UPDATE Product p SET p.isActive = false, p.sku = null, p.supplier = null WHERE p.id = :id AND p.isActive = true")
    int softDeleteAndReleaseBarcode(@Param("id") Long id);
}
