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
public class CreateSupplierInvoiceRequest {
    private Long goodsReceiptId;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String notes;
    private List<CreateSupplierInvoiceItemRequest> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateSupplierInvoiceItemRequest {
        private Long medicineId;
        private Integer quantity;
        private BigDecimal unitPrice;
        private String notes;
    }
}
