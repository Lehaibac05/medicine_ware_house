package com.pharmacy.warehouse.repository;

import com.pharmacy.warehouse.model.Batch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, Long> {

    List<Batch> findByMedicine_MedicineId(Long medicineId);
}
