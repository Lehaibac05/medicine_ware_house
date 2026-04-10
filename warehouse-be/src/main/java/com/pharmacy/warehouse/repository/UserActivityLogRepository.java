package com.pharmacy.warehouse.repository;

import com.pharmacy.warehouse.model.UserActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {
    Page<UserActivityLog> findByActionContainingIgnoreCase(String action, Pageable pageable);
}
