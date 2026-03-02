package com.pharmacy.warehouse.repository;

import com.pharmacy.warehouse.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    List<OrderItem> findByOrderOrderId(Long orderId);
    
    List<OrderItem> findByBatchBatchId(Long batchId);
}
