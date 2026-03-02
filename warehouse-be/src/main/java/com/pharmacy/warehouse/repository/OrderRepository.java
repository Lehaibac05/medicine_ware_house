package com.pharmacy.warehouse.repository;

import com.pharmacy.warehouse.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    
    List<Order> findByStatus(String status);
    
    List<Order> findByUserUserId(Long userId);
    
    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    List<Order> findByOrderDateBetween(
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.orderDate >= :date")
    List<Order> findRecentOrdersByStatus(
            @Param("status") String status, 
            @Param("date") LocalDateTime date);
}
