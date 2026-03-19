package com.pharmacy.warehouse.dto;

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
public class InventoryReportResponse {

    private List<InventoryReportItem> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private LocalDateTime generatedAt;
    private List<String> exportColumns;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InventoryReportItem {
        private Long medicineId;
        private String medicineName;
        private Long warehouseId;
        private String warehouseName;
        private Long totalStock;
        private Long batchCount;
        private LocalDate nearestExpiryDate;
        private String status;
        private Integer reorderLevel;
    }
}
