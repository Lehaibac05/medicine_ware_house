package com.pharmacy.warehouse.controller;

import com.pharmacy.warehouse.model.Batch;
import com.pharmacy.warehouse.service.BatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/medicines/{medicineId}/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','WAREHOUSE_STAFF','ACCOUNTANT')")
    public List<Batch> getByMedicine(@PathVariable Long medicineId) {
        return batchService.getByMedicine(medicineId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_STAFF')")
    public Batch create(@PathVariable Long medicineId,
                        @RequestBody Batch batch) {
        return batchService.create(medicineId, batch);
    }

    @PutMapping("/{batchId}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER')")
    public Batch update(@PathVariable Long batchId,
                        @RequestBody Batch batch) {
        return batchService.update(batchId, batch);
    }

    @DeleteMapping("/{batchId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long batchId) {
        batchService.delete(batchId);
    }
}
