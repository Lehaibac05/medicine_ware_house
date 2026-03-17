package com.pharmacy.warehouse.controller;

import com.pharmacy.warehouse.model.Medicine;
import com.pharmacy.warehouse.service.MedicineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','WAREHOUSE_STAFF','ACCOUNTANT','SUPPLIER')")
    public Page<Medicine> getMedicines(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String manufacturer,
            @RequestParam(required = false) String storageCondition,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        return medicineService.getMedicines(
                page,
                size,
                search,
                manufacturer,
                storageCondition,
                sortBy,
                sortDir
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER')")
    public Medicine create(@Valid @RequestBody Medicine medicine) {
        return medicineService.create(medicine);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER')")
    public Medicine update(@PathVariable Long id,
                           @Valid @RequestBody Medicine medicine) {
        return medicineService.update(id, medicine);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER')")
    public void delete(@PathVariable Long id) {
        medicineService.delete(id);
    }
}
