package com.pharmacy.warehouse.repository;

import com.pharmacy.warehouse.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    
    List<Alert> findByStatus(String status);
    
    List<Alert> findByAlertType(String alertType);
    
    List<Alert> findBySeverity(String severity);
    
    @Query("SELECT a FROM Alert a WHERE a.status IN ('OPEN', 'IN_PROGRESS') ORDER BY a.createdAt DESC")
    List<Alert> findActiveAlerts();
    
    @Query("SELECT a FROM Alert a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    List<Alert> findByDateRange(
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT a FROM Alert a WHERE a.alertType = :type AND a.status = 'OPEN'")
    List<Alert> findOpenAlertsByType(@Param("type") String type);
    
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.alertType = :type AND a.status IN ('OPEN', 'IN_PROGRESS')")
    Long countActiveAlertsByType(@Param("type") String type);
    
    // Check if alert already exists for a batch to avoid duplicates
    @Query("SELECT a FROM Alert a WHERE a.batch.batchId = :batchId AND a.alertType = :type AND a.status != 'RESOLVED'")
    List<Alert> findActiveAlertByBatchAndType(
            @Param("batchId") Long batchId, 
            @Param("type") String type);

    @Query("""
            SELECT a
            FROM Alert a
            WHERE a.medicine.medicineId = :medicineId
              AND a.warehouse.warehouseId = :warehouseId
              AND a.alertType = :type
              AND a.status != 'RESOLVED'
            """)
    List<Alert> findActiveAlertByMedicineWarehouseAndType(
            @Param("medicineId") Long medicineId,
            @Param("warehouseId") Long warehouseId,
            @Param("type") String type);
}
