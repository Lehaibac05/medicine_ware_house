package com.pharmacy.warehouse.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pharmacy.warehouse.model.PurchaseOrder;
import com.pharmacy.warehouse.model.PurchaseOrder.PurchaseOrderStatus;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    
    List<PurchaseOrder> findByStatus(PurchaseOrderStatus status);
    
    List<PurchaseOrder> findByWarehouse_WarehouseId(Long warehouseId);
    
    List<PurchaseOrder> findBySupplier_SupplierId(Long supplierId);
    
    @Query("SELECT po FROM PurchaseOrder po WHERE po.createdBy.userId = :userId ORDER BY po.createdAt DESC")
    List<PurchaseOrder> findByCreatedBy(@Param("userId") Long userId);
    
    @Query("SELECT po FROM PurchaseOrder po WHERE po.expectedDeliveryDate BETWEEN :startDate AND :endDate")
    List<PurchaseOrder> findByExpectedDeliveryDateBetween(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate
    );
    
    @Query("SELECT po FROM PurchaseOrder po LEFT JOIN FETCH po.items WHERE po.purchaseOrderId = :id")
    PurchaseOrder findByIdWithItems(@Param("id") Long id);
}
