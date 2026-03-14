package com.pharmacy.warehouse.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderResponse {
    private Long purchaseOrderId;
    private String orderCode;
    private SupplierResponse supplier;
    private WarehouseInfo warehouse;
    private UserInfo createdBy;
    private String status;
    private LocalDate expectedDeliveryDate;
    private BigDecimal totalAmount;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PurchaseOrderItemResponse> items;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PurchaseOrderItemResponse {
        private Long itemId;
        private MedicineInfo medicine;
        private Integer requestedQuantity;
        private Integer receivedQuantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private LocalDate expectedExpiryDate;
        private LocalDate actualExpiryDate;
        private String notes;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MedicineInfo {
        private Long medicineId;
        private String medicineName;
        private String sku;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WarehouseInfo {
        private Long warehouseId;
        private String warehouseName;
    }
    
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
