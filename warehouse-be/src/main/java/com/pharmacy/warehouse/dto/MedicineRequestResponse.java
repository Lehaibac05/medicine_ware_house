package com.pharmacy.warehouse.dto;

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
public class MedicineRequestResponse {
    private Long requestId;
    private Long warehouseId;
    private String warehouseName;
    private LocalDate requiredDate;
    private String notes;
    private String requestedBy;
    private LocalDateTime createdDate;
    private String status;
    private List<ItemResponse> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemResponse {
        private Long medicineId;
        private String medicineName;
        private Integer quantity;
        private String notes;
    }
}
