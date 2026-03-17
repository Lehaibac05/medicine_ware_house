package com.pharmacy.warehouse.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pharmacy.warehouse.dto.ExpiringBatchResponse;
import com.pharmacy.warehouse.dto.InventoryDetailResponse;
import com.pharmacy.warehouse.dto.InventoryResponse;
import com.pharmacy.warehouse.dto.InventorySummaryResponse;
import com.pharmacy.warehouse.service.InventoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','WAREHOUSE_STAFF','ACCOUNTANT')")
    public List<InventoryResponse> getInventory(
            @RequestParam(required = false) String medicineName,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryTo,
            @RequestParam(required = false) String status) {
        log.info("GET /inventory");
        return inventoryService.getInventory(medicineName, warehouseId, expiryFrom, expiryTo, status);
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','WAREHOUSE_STAFF','ACCOUNTANT')")
    public Page<InventoryResponse> getInventoryPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String medicineName,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryTo,
            @RequestParam(required = false) String status) {
        log.info("GET /inventory/paged page={}, size={}", page, size);
        return inventoryService.getInventoryPaged(page, size, medicineName, warehouseId, expiryFrom, expiryTo, status);
    }

    @GetMapping("/{medicineId}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','WAREHOUSE_STAFF','ACCOUNTANT')")
    public List<InventoryDetailResponse> getInventoryDetailByMedicine(@PathVariable Long medicineId) {
        log.info("GET /inventory/{}", medicineId);
        return inventoryService.getInventoryDetailByMedicine(medicineId);
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','WAREHOUSE_STAFF','ACCOUNTANT')")
    public List<InventoryResponse> getLowStockInventory() {
        log.info("GET /inventory/low-stock");
        return inventoryService.getLowStockInventory();
    }

    @GetMapping("/expiring")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','WAREHOUSE_STAFF','ACCOUNTANT')")
    public List<ExpiringBatchResponse> getExpiringBatches() {
        log.info("GET /inventory/expiring");
        return inventoryService.getExpiringBatches();
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','WAREHOUSE_STAFF','ACCOUNTANT')")
    public InventorySummaryResponse getInventorySummary() {
        log.info("GET /inventory/summary");
        return inventoryService.getInventorySummary();
    }
}
