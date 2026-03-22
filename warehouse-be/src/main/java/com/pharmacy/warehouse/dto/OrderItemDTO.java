package com.pharmacy.warehouse.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private Long orderItemId;
    private Long orderId;
    private Long medicineId;
    private Long batchId;
    private String medicineName;
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
