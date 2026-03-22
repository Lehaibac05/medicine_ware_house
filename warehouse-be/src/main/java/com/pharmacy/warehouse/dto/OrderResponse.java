package com.pharmacy.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long orderId;
    private Long medicineId;
    private String medicineName;
    private Integer requestedQuantity;
    private Integer approvedQuantity;
    private Integer issuedQuantity;
    private Integer remainingQuantity;
    private String department;
    private String purpose;
    private Long warehouseId;
    private String warehouseName;
    private LocalDateTime requestDate;
    private LocalDateTime neededDate;
    private String status;
    private String rejectionReason;
    private Long createdById;
    private String createdByName;
    private Long approvedById;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer availableStockInWarehouse;
    private Integer suggestedAvailableQuantity;
    private List<StockOption> alternativeWarehouses;
    private List<OrderItemDTO> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockOption {
        private Long warehouseId;
        private String warehouseName;
        private Integer availableQuantity;
    }
}
