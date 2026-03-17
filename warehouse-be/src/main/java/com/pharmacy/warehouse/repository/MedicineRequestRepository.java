package com.pharmacy.warehouse.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pharmacy.warehouse.model.MedicineRequest;

public interface MedicineRequestRepository extends JpaRepository<MedicineRequest, Long> {

    @Query("SELECT mr FROM MedicineRequest mr LEFT JOIN FETCH mr.items i LEFT JOIN FETCH i.medicine LEFT JOIN FETCH mr.warehouse LEFT JOIN FETCH mr.requestedBy ORDER BY mr.createdDate DESC")
    List<MedicineRequest> findAllWithItems();

    @Query("SELECT mr FROM MedicineRequest mr LEFT JOIN FETCH mr.items i LEFT JOIN FETCH i.medicine LEFT JOIN FETCH mr.warehouse LEFT JOIN FETCH mr.requestedBy WHERE mr.requestId = :id")
    MedicineRequest findByIdWithItems(@Param("id") Long id);

    @Query("SELECT mr FROM MedicineRequest mr LEFT JOIN FETCH mr.items i LEFT JOIN FETCH i.medicine LEFT JOIN FETCH mr.warehouse LEFT JOIN FETCH mr.requestedBy WHERE mr.requestedBy.userId = :userId ORDER BY mr.createdDate DESC")
    List<MedicineRequest> findAllByRequestedByUserIdWithItems(@Param("userId") Long userId);
}
