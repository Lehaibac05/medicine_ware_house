package com.pharmacy.warehouse.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pharmacy.warehouse.dto.SupplierRequest;
import com.pharmacy.warehouse.dto.SupplierResponse;
import com.pharmacy.warehouse.model.Supplier.SupplierStatus;
import com.pharmacy.warehouse.service.SupplierService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public ResponseEntity<Page<SupplierResponse>> getSuppliers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /suppliers - search suppliers");

        SupplierStatus supplierStatus = null;
        if (status != null) {
            supplierStatus = SupplierStatus.valueOf(status.toUpperCase());
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<SupplierResponse> result = supplierService.searchSuppliers(
                supplierStatus,
                supplierName,
                keyword,
                pageable);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/active")
    public ResponseEntity<List<SupplierResponse>> getActiveSuppliers() {
        log.info("GET /suppliers/active - Fetching active suppliers");
        List<SupplierResponse> suppliers = supplierService.getActiveSuppliers();
        return ResponseEntity.ok(suppliers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponse> getSupplierById(@PathVariable Long id) {
        log.info("GET /suppliers/{} - Fetching supplier", id);
        SupplierResponse supplier = supplierService.getSupplierById(id);
        return ResponseEntity.ok(supplier);
    }

    @PostMapping
    public ResponseEntity<SupplierResponse> createSupplier(
            @RequestBody SupplierRequest request,
            Authentication authentication) {
        log.info("POST /suppliers - Creating new supplier by user: {}",
                authentication != null ? authentication.getName() : "unknown");
        SupplierResponse supplier = supplierService.createSupplier(request);
        return ResponseEntity.ok(supplier);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponse> updateSupplier(
            @PathVariable Long id,
            @RequestBody SupplierRequest request,
            Authentication authentication) {
        log.info("PUT /suppliers/{} - Updating supplier by user: {}",
                id, authentication != null ? authentication.getName() : "unknown");
        SupplierResponse supplier = supplierService.updateSupplier(id, request);
        return ResponseEntity.ok(supplier);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(
            @PathVariable Long id,
            Authentication authentication) {
        log.info("DELETE /suppliers/{} - Deleting supplier by user: {}",
                id, authentication != null ? authentication.getName() : "unknown");
        supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }
}
