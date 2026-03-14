package com.pharmacy.warehouse.repository;

import java.util.List;
import java.util.Optional;

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
}
