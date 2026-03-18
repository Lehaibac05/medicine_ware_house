package com.pharmacy.warehouse.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pharmacy.warehouse.dto.DashboardSummaryResponse;
import com.pharmacy.warehouse.dto.FinancialReportResponse;
import com.pharmacy.warehouse.dto.InventoryReportResponse;
import com.pharmacy.warehouse.service.ReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/inventory")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','WAREHOUSE_STAFF','ACCOUNTANT')")
    public ResponseEntity<InventoryReportResponse> getInventoryReport(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String medicineName,
            @RequestParam(required = false) String medicineGroup,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(reportService.getInventoryReport(
                warehouseId,
                medicineName,
                medicineGroup,
                status,
                expiryFrom,
                expiryTo,
                page,
                size));
    }

    @GetMapping("/inventory/export")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','WAREHOUSE_STAFF','ACCOUNTANT')")
    public ResponseEntity<InventoryReportResponse> exportInventoryReport(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String medicineName,
            @RequestParam(required = false) String medicineGroup,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryTo) {

        return ResponseEntity.ok(reportService.exportInventoryReport(
                warehouseId,
                medicineName,
                medicineGroup,
                status,
                expiryFrom,
                expiryTo));
    }

    @GetMapping("/financial")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','WAREHOUSE_MANAGER')")
    public ResponseEntity<FinancialReportResponse> getFinancialReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(reportService.getFinancialReport(
                fromDate,
                toDate,
                supplierId,
                status,
                page,
                size));
    }

    @GetMapping("/financial/export")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','WAREHOUSE_MANAGER')")
    public ResponseEntity<FinancialReportResponse> exportFinancialReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status) {

        return ResponseEntity.ok(reportService.exportFinancialReport(fromDate, toDate, supplierId, status));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','ACCOUNTANT')")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary() {
        return ResponseEntity.ok(reportService.getDashboardSummary());
    }
}
