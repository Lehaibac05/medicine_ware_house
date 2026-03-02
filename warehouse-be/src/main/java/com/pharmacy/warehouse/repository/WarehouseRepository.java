package com.pharmacy.warehouse.repository;

import com.pharmacy.warehouse.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
}
