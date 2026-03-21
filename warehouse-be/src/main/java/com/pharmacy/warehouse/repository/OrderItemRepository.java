package com.pharmacy.warehouse.repository;

import com.pharmacy.warehouse.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    List<OrderItem> findByOrderOrderId(Long orderId);
    
    List<OrderItem> findByBatchBatchId(Long batchId);

        @Query("""
                        SELECT oi FROM OrderItem oi
                        JOIN FETCH oi.order o
                        JOIN FETCH o.medicine m
                        JOIN FETCH oi.batch b
                        JOIN FETCH oi.warehouse w
                        JOIN FETCH oi.issuedBy ib
                        WHERE (:medicineId IS NULL OR m.medicineId = :medicineId)
                            AND (:department IS NULL OR LOWER(o.department) LIKE LOWER(CONCAT('%', :department, '%')))
                            AND (:warehouseId IS NULL OR w.warehouseId = :warehouseId)
                            AND (:issuedById IS NULL OR ib.userId = :issuedById)
                            AND (:fromDate IS NULL OR oi.issuedAt >= :fromDate)
                            AND (:toDate IS NULL OR oi.issuedAt <= :toDate)
                        ORDER BY oi.issuedAt DESC
                        """)
        List<OrderItem> findIssueHistory(
                        @Param("medicineId") Long medicineId,
                        @Param("department") String department,
                        @Param("warehouseId") Long warehouseId,
                        @Param("issuedById") Long issuedById,
                        @Param("fromDate") LocalDateTime fromDate,
                        @Param("toDate") LocalDateTime toDate);
}
