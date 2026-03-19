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
public class FinancialReportResponse {

    private List<FinancialReportItem> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private BigDecimal totalInvoiceAmount;
    private BigDecimal totalPaidAmount;
    private BigDecimal totalRemainingAmount;
    private LocalDateTime generatedAt;
    private List<String> exportColumns;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FinancialReportItem {
        private Long invoiceId;
        private String invoiceCode;
        private Long supplierId;
        private String supplierName;
        private LocalDate invoiceDate;
        private LocalDate dueDate;
        private BigDecimal totalAmount;
        private BigDecimal paidAmount;
        private BigDecimal remainingAmount;
        private String status;
    }
}
