package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.model.Medicine;
import com.pharmacy.warehouse.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRepository medicineRepository;

    public List<Medicine> getAll() {
        return medicineRepository.findAll();
    }

    public Medicine getById(Long id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medicine not found"));
    }

    public Medicine create(Medicine medicine) {
        return medicineRepository.save(medicine);
    }

    public Medicine update(Long id, Medicine data) {
        Medicine m = getById(id);
        m.setName(data.getName());
        m.setManufacturer(data.getManufacturer());
        m.setStorageCondition(data.getStorageCondition());
        m.setDescription(data.getDescription());
        return medicineRepository.save(m);
    }

    public void delete(Long id) {
        medicineRepository.deleteById(id);
    }
}
