package com.pharmacy.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueReportResponse {
    private List<IssueReportItem> items;
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
    public static class IssueReportItem {
        private Long orderItemId;
        private Long requestId;
        private Long medicineId;
        private String medicineName;
        private Long batchId;
        private String lotNumber;
        private LocalDate expiryDate;
        private Integer quantity;
        private Long warehouseId;
        private String warehouseName;
        private Long issuedById;
        private String issuedByName;
        private String department;
        private LocalDateTime issuedAt;
    }
}
