package com.pharmacy.warehouse.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "alert")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long alertId;

    private String alertType; // LOW_STOCK, EXPIRING_SOON, EXPIRED, SYSTEM
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private String status; // OPEN, IN_PROGRESS, RESOLVED
    private String message;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    
    @ManyToOne
    @JoinColumn(name = "batch_id")
    private Batch batch;
    
    @ManyToOne
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;
    
    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;
    
    @ManyToOne
    @JoinColumn(name = "resolved_by_user_id")
    @JsonIgnore
    private User resolvedBy;
}
