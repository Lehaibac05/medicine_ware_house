package com.pharmacy.warehouse.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pharmacy.warehouse.model.PurchaseOrderItem;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {
    
    List<PurchaseOrderItem> findByPurchaseOrder_PurchaseOrderId(Long purchaseOrderId);
    
    List<PurchaseOrderItem> findByMedicine_MedicineId(Long medicineId);
    
    @Query("SELECT poi FROM PurchaseOrderItem poi WHERE poi.purchaseOrder.purchaseOrderId = :orderId AND poi.medicine.medicineId = :medicineId")
    PurchaseOrderItem findByOrderAndMedicine(
        @Param("orderId") Long orderId, 
        @Param("medicineId") Long medicineId
    );
}
