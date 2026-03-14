package com.pharmacy.warehouse.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptResponse {
    private Long receiptId;
    private String receiptCode;
    private PurchaseOrderResponse purchaseOrder;
    private UserInfo receivedBy;
    private UserInfo approvedBy;
    private String status;
    private String qualityCheckNotes;
    private Boolean qualityPassed;
    private LocalDateTime receivedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
        private Long userId;
        private String username;
        private String fullName;
    }
}
