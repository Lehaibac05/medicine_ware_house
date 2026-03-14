package com.pharmacy.warehouse.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "purchase_order_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    @ManyToOne
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;

    @ManyToOne
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    private Integer requestedQuantity;  // Số lượng yêu cầu
    private Integer receivedQuantity;   // Số lượng thực nhận (Bước 3)
    
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    
    private LocalDate expectedExpiryDate; // Hạn sử dụng mong muốn
    private LocalDate actualExpiryDate;   // Hạn sử dụng thực tế (Bước 3)
    
    private String notes;
}