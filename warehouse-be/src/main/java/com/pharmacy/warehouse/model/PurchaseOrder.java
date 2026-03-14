package com.pharmacy.warehouse.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "purchase_order")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long purchaseOrderId;

    private String orderCode; // PO-2026-0001

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy; // WAREHOUSE_MANAGER

    @Enumerated(EnumType.STRING)
    private PurchaseOrderStatus status;

    private LocalDate expectedDeliveryDate;
    private BigDecimal totalAmount;
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL)
    private List<PurchaseOrderItem> items;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (orderCode == null) {
            orderCode = generateOrderCode();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateOrderCode() {
        return "PO-" + LocalDate.now().getYear() + "-" + 
               String.format("%04d", System.currentTimeMillis() % 10000);
    }

    public enum PurchaseOrderStatus {
        PENDING,      // Bước 1: Vừa tạo
        CONFIRMED,    // Nhà cung cấp xác nhận
        PREPARING,    // Bước 2: Đang chuẩn bị
        SHIPPING,     // Đang giao hàng
        RECEIVED,     // Bước 3: Đã nhận hàng
        APPROVED,     // Bước 4: Đã phê duyệt
        CANCELLED     // Hủy
    }
}