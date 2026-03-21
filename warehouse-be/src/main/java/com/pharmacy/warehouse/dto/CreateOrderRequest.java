package com.pharmacy.warehouse.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    private Long medicineId;
    private Integer quantity;
    private Long warehouseId;
    private String department;
    private String purpose;
    private LocalDateTime neededDate;
}
