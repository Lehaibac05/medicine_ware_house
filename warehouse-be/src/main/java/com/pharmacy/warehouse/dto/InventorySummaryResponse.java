package com.pharmacy.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventorySummaryResponse {
    private Long totalMedicines;
    private Long totalBatches;
    private Long lowStockCount;
    private Long expiringSoonCount;
}
