package com.pharmacy.warehouse.repository;

import com.pharmacy.warehouse.model.AlertHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {
    
    List<AlertHistory> findByAlertAlertIdOrderByTimestampDesc(Long alertId);
    
    @Query("SELECT ah FROM AlertHistory ah WHERE ah.alert.alertId = :alertId ORDER BY ah.timestamp DESC")
    List<AlertHistory> findHistoryByAlertId(Long alertId);
}
