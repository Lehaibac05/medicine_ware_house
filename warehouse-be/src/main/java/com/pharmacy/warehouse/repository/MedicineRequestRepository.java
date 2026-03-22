package com.pharmacy.warehouse.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pharmacy.warehouse.model.MedicineRequest;

public interface MedicineRequestRepository extends JpaRepository<MedicineRequest, Long>, JpaSpecificationExecutor<MedicineRequest> {

    @Query("SELECT DISTINCT mr FROM MedicineRequest mr LEFT JOIN FETCH mr.items i LEFT JOIN FETCH i.medicine LEFT JOIN FETCH mr.warehouse LEFT JOIN FETCH mr.requestedBy WHERE mr.requestId = :id")
    MedicineRequest findByIdWithItems(@Param("id") Long id);

    @Query(value = "SELECT mr FROM MedicineRequest mr LEFT JOIN FETCH mr.items i LEFT JOIN FETCH i.medicine LEFT JOIN FETCH mr.warehouse LEFT JOIN FETCH mr.requestedBy",
           countQuery = "SELECT count(mr.requestId) FROM MedicineRequest mr")
    Page<MedicineRequest> findAll(Pageable pageable);

    @Query("SELECT DISTINCT mr FROM MedicineRequest mr LEFT JOIN FETCH mr.items i LEFT JOIN FETCH i.medicine LEFT JOIN FETCH mr.warehouse LEFT JOIN FETCH mr.requestedBy WHERE mr.requestId IN :requestIds")
    List<MedicineRequest> findAllByRequestIdInWithDetails(@Param("requestIds") List<Long> requestIds);

}
