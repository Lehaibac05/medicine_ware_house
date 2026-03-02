package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.model.Batch;
import com.pharmacy.warehouse.model.Medicine;
import com.pharmacy.warehouse.model.Warehouse;
import com.pharmacy.warehouse.repository.BatchRepository;
import com.pharmacy.warehouse.repository.MedicineRepository;
import com.pharmacy.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BatchService {

    private final BatchRepository batchRepository;
    private final MedicineRepository medicineRepository;
    private final WarehouseRepository warehouseRepository;

    public List<Batch> getAll() {
        return batchRepository.findAll();
    }

    public List<Batch> getByMedicine(Long medicineId) {
        return batchRepository.findByMedicine_MedicineId(medicineId);
    }

    public Batch create(Long medicineId, Batch batch) {
        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new RuntimeException("Medicine not found"));

        batch.setMedicine(medicine);
        applyWarehouseIfPresent(batch);
        return batchRepository.save(batch);
    }

    public Batch update(Long id, Batch data) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch not found"));

        if (data.getLotNumber() != null) {
            batch.setLotNumber(data.getLotNumber());
        }
        if (data.getManufactureDate() != null) {
            batch.setManufactureDate(data.getManufactureDate());
        }
        if (data.getExpiryDate() != null) {
            batch.setExpiryDate(data.getExpiryDate());
        }
        if (data.getQuantity() != null) {
            batch.setQuantity(data.getQuantity());
        }
        if (data.getStatus() != null) {
            batch.setStatus(data.getStatus());
        }
        if (data.getWarehouse() != null) {
            setWarehouse(batch, data.getWarehouse());
        }

        return batchRepository.save(batch);
    }

    public void delete(Long id) {
        batchRepository.deleteById(id);
    }

    private void applyWarehouseIfPresent(Batch batch) {
        if (batch.getWarehouse() == null) {
            return;
        }
        setWarehouse(batch, batch.getWarehouse());
    }

    private void setWarehouse(Batch batch, Warehouse warehouse) {
        if (warehouse.getWarehouseId() == null) {
            throw new RuntimeException("Warehouse id is required");
        }
        Warehouse managedWarehouse = warehouseRepository.findById(warehouse.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));
        batch.setWarehouse(managedWarehouse);
    }
}
