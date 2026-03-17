package com.pharmacy.warehouse.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    private Long medicineId;
    private String medicineName;
    private Long warehouseId;
    private String warehouseName;
    private Long totalStock;
    private Long batchCount;
    private LocalDate nearestExpiryDate;
    private String status;
}
