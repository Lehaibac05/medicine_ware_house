package com.pharmacy.warehouse.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDetailResponse {
    private Long medicineId;
    private String medicineName;
    private Long warehouseId;
    private String warehouseName;
    private Long totalStock;
    private List<BatchInfo> batches;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchInfo {
        private Long batchId;
        private String lotNumber;
        private Integer quantity;
        private LocalDate manufactureDate;
        private LocalDate expiryDate;
    }
}
