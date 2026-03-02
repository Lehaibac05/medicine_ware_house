package com.pharmacy.warehouse.controller;

import com.pharmacy.warehouse.model.Medicine;
import com.pharmacy.warehouse.service.MedicineService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','WAREHOUSE_STAFF','ACCOUNTANT','SUPPLIER')")
    public List<Medicine> getAll() {
        return medicineService.getAll();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER')")
    public Medicine create(@RequestBody Medicine medicine) {
        return medicineService.create(medicine);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER')")
    public Medicine update(@PathVariable Long id,
                           @RequestBody Medicine medicine) {
        return medicineService.update(id, medicine);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER')")
    public void delete(@PathVariable Long id) {
        medicineService.delete(id);
    }
}
