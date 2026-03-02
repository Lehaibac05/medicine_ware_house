package com.pharmacy.warehouse.model;

import java.time.LocalDateTime;

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
@Table(name = "alert_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;
    
    @ManyToOne
    @JoinColumn(name = "alert_id")
    private Alert alert;
    
    private String action; // CREATED, STATUS_CHANGED, RESOLVED, REOPENED
    private String oldStatus;
    private String newStatus;
    private String comment;
    private LocalDateTime timestamp;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
