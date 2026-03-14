package com.pharmacy.warehouse.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "goods_receipt")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long receiptId;

    private String receiptCode; // GR-2026-0001

    @OneToOne
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;

    @ManyToOne
    @JoinColumn(name = "received_by")
    private User receivedBy; // WAREHOUSE_STAFF - Bước 3

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private User approvedBy; // WAREHOUSE_MANAGER - Bước 4

    @Enumerated(EnumType.STRING)
    private ReceiptStatus status;

    private String qualityCheckNotes; // Ghi chú kiểm tra chất lượng
    private Boolean qualityPassed;    // Đạt/Không đạt
    
    @Column(name = "received_at")
    private LocalDateTime receivedAt; // Thời gian nhận hàng

    @Column(name = "approved_at")
    private LocalDateTime approvedAt; // Thời gian phê duyệt

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (receiptCode == null) {
            receiptCode = generateReceiptCode();
        }
    }

    private String generateReceiptCode() {
        return "GR-" + java.time.LocalDate.now().getYear() + "-" + 
               String.format("%04d", System.currentTimeMillis() % 10000);
    }

    public enum ReceiptStatus {
        PENDING_APPROVAL, // Bước 3: Chờ phê duyệt
        APPROVED,         // Bước 4: Đã phê duyệt
        REJECTED          // Từ chối
    }
}