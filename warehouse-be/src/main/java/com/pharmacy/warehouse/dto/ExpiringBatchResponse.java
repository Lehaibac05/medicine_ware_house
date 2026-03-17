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
public class ExpiringBatchResponse {
    private Long batchId;
    private String lotNumber;
    private Integer quantity;
    private LocalDate manufactureDate;
    private LocalDate expiryDate;
    private Long medicineId;
    private String medicineName;
    private Long warehouseId;
    private String warehouseName;
}
