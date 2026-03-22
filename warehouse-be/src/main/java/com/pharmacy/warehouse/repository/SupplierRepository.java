package com.pharmacy.warehouse.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pharmacy.warehouse.model.Supplier;
import com.pharmacy.warehouse.model.Supplier.SupplierStatus;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByStatus(SupplierStatus status);

    Optional<Supplier> findByTaxCode(String taxCode);

    @Query("SELECT s FROM Supplier s WHERE s.status = 'ACTIVE' ORDER BY s.supplierName")
    List<Supplier> findAllActiveSuppliers();

    @Query("""
                SELECT s FROM Supplier s
                WHERE (:status IS NULL OR s.status = :status)
                AND (:supplierName IS NULL OR LOWER(s.supplierName) LIKE LOWER(CONCAT('%', :supplierName, '%')))
                AND (
                    :keyword IS NULL OR
                    LOWER(s.supplierName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                    LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
            """)
    Page<Supplier> searchSuppliers(
            @Param("status") SupplierStatus status,
            @Param("supplierName") String supplierName,
            @Param("keyword") String keyword,
            Pageable pageable);
}
