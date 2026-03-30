package com.pharmacy.warehouse.model;

import java.time.LocalDateTime;
import java.util.List;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "supplier")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long supplierId;

    private String supplierName;
    private String contactPerson;
    private String phoneNumber;
    private String email;
    private String address;
    private String taxCode;

    /**
     * Link hoặc chuỗi nội dung để hiển thị/sinh QR chuyển khoản.
     * Frontend sẽ tự quyết định hiển thị dưới dạng ảnh hay sinh QR từ chuỗi.
     */
    @Column(name = "qr_bank_transfer_link", length = 2000)
    private String qrBankTransferLink;
    
    @Enumerated(EnumType.STRING)
    private SupplierStatus status; // ACTIVE, INACTIVE, SUSPENDED

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum SupplierStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED
    }
}