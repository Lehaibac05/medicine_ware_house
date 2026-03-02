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
@Table(name = "alert")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long alertId;

    private String type;
    private String message;
    private String severity;
    private LocalDateTime createdDate;

    @ManyToOne
    @JoinColumn(name = "model_id")
    private AIModel model;
}
