package com.pharmacy.warehouse.repository.projection;

import java.time.LocalDate;

public interface InventoryAggregateProjection {
    Long getMedicineId();
    String getMedicineName();
    Integer getReorderLevel();
    Long getWarehouseId();
    String getWarehouseName();
    Long getTotalStock();
    Long getBatchCount();
    LocalDate getNearestExpiryDate();
}
