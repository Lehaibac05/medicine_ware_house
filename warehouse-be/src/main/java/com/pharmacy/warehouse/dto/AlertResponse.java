package com.pharmacy.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {
    private Long alertId;
    private String alertType;
    private String severity;
    private String status;
    private String message;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    
    // Related entities
    private Long batchId;
    private String lotNumber;
    private Long medicineId;
    private String medicineName;
    private Long warehouseId;
    private String warehouseName;
    
    // Resolved by
    private Long resolvedByUserId;
    private String resolvedByUsername;
}
