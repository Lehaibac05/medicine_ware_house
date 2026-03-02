package com.pharmacy.warehouse.model;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonBackReference;

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
@Table(name = "batch")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long batchId;

    private String lotNumber;
    private LocalDate manufactureDate;
    private LocalDate expiryDate;
    private Integer quantity;
    private String status;

    @ManyToOne
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;
}
