package com.pharmacy.warehouse.repository;

import com.pharmacy.warehouse.model.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MedicineRepository extends JpaRepository<Medicine, Long>, JpaSpecificationExecutor<Medicine> {
	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndMedicineIdNot(String name, Long medicineId);
}
