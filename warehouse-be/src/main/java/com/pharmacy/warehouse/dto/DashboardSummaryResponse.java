package com.pharmacy.warehouse.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {

    private long totalMedicines;
    private long totalStock;
    private long lowStockCount;
    private long expiringSoonCount;
    private long totalInvoices;
    private BigDecimal totalPaid;
    private BigDecimal totalUnpaid;
    private List<WarehouseStockPoint> stockByWarehouse;
    private List<InvoiceStatusPoint> invoiceStatusDistribution;
    private List<MonthlySpendingPoint> monthlySpending;
    private List<LowStockAlertItem> lowStockItems;
    private List<ExpiringBatchAlertItem> expiringBatchItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WarehouseStockPoint {
        private Long warehouseId;
        private String warehouseName;
        private Long totalStock;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvoiceStatusPoint {
        private String status;
        private Long value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlySpendingPoint {
        private String month;
        private BigDecimal amount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LowStockAlertItem {
        private Long medicineId;
        private String medicineName;
        private Long warehouseId;
        private String warehouseName;
        private Long totalStock;
        private Integer reorderLevel;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExpiringBatchAlertItem {
        private Long batchId;
        private String lotNumber;
        private Long medicineId;
        private String medicineName;
        private Long warehouseId;
        private String warehouseName;
        private LocalDate expiryDate;
        private Integer quantity;
    }
}
