package com.pharmacy.warehouse.repository;

import com.pharmacy.warehouse.model.Batch;
import com.pharmacy.warehouse.repository.projection.InventoryAggregateProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, Long> {

    List<Batch> findByMedicine_MedicineId(Long medicineId);

    @Query("""
        SELECT m.medicineId AS medicineId,
           m.name AS medicineName,
               m.reorderLevel AS reorderLevel,
           w.warehouseId AS warehouseId,
           w.name AS warehouseName,
           COALESCE(SUM(b.quantity), 0) AS totalStock,
           COUNT(b.batchId) AS batchCount,
           MIN(b.expiryDate) AS nearestExpiryDate
        FROM Batch b
        JOIN b.medicine m
        JOIN b.warehouse w
        WHERE b.quantity > 0
          AND (:medicineName IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :medicineName, '%')))
                    AND (:medicineGroup IS NULL OR LOWER(m.manufacturer) LIKE LOWER(CONCAT('%', :medicineGroup, '%')))
          AND (:warehouseId IS NULL OR w.warehouseId = :warehouseId)
          AND (:expiryFrom IS NULL OR b.expiryDate >= :expiryFrom)
          AND (:expiryTo IS NULL OR b.expiryDate <= :expiryTo)
            GROUP BY m.medicineId, m.name, m.reorderLevel, w.warehouseId, w.name
        ORDER BY m.name ASC, w.name ASC
        """)
    List<InventoryAggregateProjection> aggregateInventory(
        @Param("medicineName") String medicineName,
        @Param("medicineGroup") String medicineGroup,
        @Param("warehouseId") Long warehouseId,
        @Param("expiryFrom") LocalDate expiryFrom,
        @Param("expiryTo") LocalDate expiryTo);

    @Query("""
        SELECT b
        FROM Batch b
        JOIN FETCH b.medicine m
        JOIN FETCH b.warehouse w
        WHERE b.medicine.medicineId = :medicineId
          AND b.quantity > 0
        ORDER BY w.name ASC, b.expiryDate ASC
        """)
    List<Batch> findInventoryDetailsByMedicine(@Param("medicineId") Long medicineId);

    @Query("""
        SELECT b
        FROM Batch b
        JOIN FETCH b.medicine m
        JOIN FETCH b.warehouse w
        WHERE b.quantity > 0
          AND b.expiryDate IS NOT NULL
          AND b.expiryDate <= :expiryThreshold
        ORDER BY b.expiryDate ASC
        """)
    List<Batch> findExpiringBatches(@Param("expiryThreshold") LocalDate expiryThreshold);
}
