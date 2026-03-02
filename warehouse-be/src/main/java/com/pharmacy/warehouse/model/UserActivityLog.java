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
@Table(name = "user_activity_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    private String action;
    private String targetObject;
    private LocalDateTime timestamp;
    private String ipAddress;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
