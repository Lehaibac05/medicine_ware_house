package com.pharmacy.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertStatsResponse {
    private Long lowStockCount;
    private Long expiringSoonCount;
    private Long expiredCount;
    private Long systemWarningsCount;
    private Long totalActiveAlerts;
}
