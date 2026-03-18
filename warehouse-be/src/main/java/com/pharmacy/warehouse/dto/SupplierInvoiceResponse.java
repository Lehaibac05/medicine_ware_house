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
public class SupplierInvoiceResponse {
    private Long invoiceId;
    private String invoiceCode;
    private SupplierResponse supplier;
    private GoodsReceiptInfo goodsReceipt;
    private String status;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private String notes;
    private String verificationNotes;
    private String rejectionReason;
    private Boolean hasMismatch;
    private String mismatchWarning;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime paidAt;
    private UserInfo createdBy;
    private UserInfo verifiedBy;
    private UserInfo paidBy;
    private List<SupplierInvoiceItemResponse> items;
    private List<PaymentInfo> payments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SupplierInvoiceItemResponse {
        private Long itemId;
        private MedicineInfo medicine;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MedicineInfo {
        private Long medicineId;
        private String medicineName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GoodsReceiptInfo {
        private Long receiptId;
        private String receiptCode;
        private String purchaseOrderCode;
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentInfo {
        private Long paymentId;
        private LocalDateTime paymentDate;
        private BigDecimal amount;
        private String method;
        private String transactionReference;
        private String status;
        private String notes;
    }
}
