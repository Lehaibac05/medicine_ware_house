package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.model.Warehouse;
import com.pharmacy.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public List<Warehouse> getAll() {
        return warehouseRepository.findAll();
    }

    public Warehouse getById(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));
    }

    public Warehouse create(Warehouse warehouse) {
        return warehouseRepository.save(warehouse);
    }

    public Warehouse update(Long id, Warehouse data) {
        Warehouse warehouse = getById(id);
        warehouse.setName(data.getName());
        warehouse.setLocation(data.getLocation());
        warehouse.setDescription(data.getDescription());
        return warehouseRepository.save(warehouse);
    }

    public void delete(Long id) {
        warehouseRepository.deleteById(id);
    }
}
