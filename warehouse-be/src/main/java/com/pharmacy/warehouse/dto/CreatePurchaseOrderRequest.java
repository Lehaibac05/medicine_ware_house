package com.pharmacy.warehouse.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePurchaseOrderRequest {
    private Long supplierId;
    private Long warehouseId;
    private LocalDate expectedDeliveryDate;
    private String notes;
    private List<PurchaseOrderItemRequest> items;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseOrderItemRequest {
        private Long medicineId;
        private Integer requestedQuantity;
        private BigDecimal unitPrice;
        private LocalDate expectedExpiryDate;
        private String notes;
    }
}
