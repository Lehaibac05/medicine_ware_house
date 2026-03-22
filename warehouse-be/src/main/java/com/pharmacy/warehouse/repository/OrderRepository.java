package com.pharmacy.warehouse.repository;

import com.pharmacy.warehouse.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByStatus(Order.IssueRequestStatus status);

    List<Order> findByCreatedByUserId(Long userId);

    @Query("SELECT o FROM Order o JOIN FETCH o.medicine m JOIN FETCH o.createdBy cb LEFT JOIN FETCH o.approvedBy ab LEFT JOIN FETCH o.warehouse w ORDER BY o.createdAt DESC")
    List<Order> findAllIssueRequests();

    @Query("SELECT o FROM Order o JOIN FETCH o.medicine m JOIN FETCH o.createdBy cb LEFT JOIN FETCH o.approvedBy ab LEFT JOIN FETCH o.warehouse w WHERE o.orderId = :id")
    Order findIssueRequestById(@Param("id") Long id);

    @Query("SELECT o FROM Order o JOIN FETCH o.medicine m JOIN FETCH o.createdBy cb LEFT JOIN FETCH o.approvedBy ab LEFT JOIN FETCH o.warehouse w WHERE o.createdBy.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findIssueRequestsByUser(@Param("userId") Long userId);
}
