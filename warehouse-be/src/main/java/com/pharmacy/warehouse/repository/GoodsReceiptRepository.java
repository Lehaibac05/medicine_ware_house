package com.pharmacy.warehouse.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pharmacy.warehouse.model.GoodsReceipt;
import com.pharmacy.warehouse.model.GoodsReceipt.ReceiptStatus;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {
    
    Optional<GoodsReceipt> findByPurchaseOrder_PurchaseOrderId(Long purchaseOrderId);
    
    List<GoodsReceipt> findByStatus(ReceiptStatus status);
    
    List<GoodsReceipt> findByReceivedBy_UserId(Long userId);
    
    List<GoodsReceipt> findByApprovedBy_UserId(Long userId);
    
    @Query("SELECT gr FROM GoodsReceipt gr WHERE gr.status = 'PENDING_APPROVAL' ORDER BY gr.receivedAt DESC")
    List<GoodsReceipt> findPendingReceipts();
    
    @Query("SELECT gr FROM GoodsReceipt gr WHERE gr.receivedAt BETWEEN :startDate AND :endDate")
    List<GoodsReceipt> findByReceivedAtBetween(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );
}
