package com.pharmacy.warehouse.model;

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
@Table(name = "medicine_request_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicineRequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long requestItemId;

    @ManyToOne
    @JoinColumn(name = "request_id")
    private MedicineRequest request;

    @ManyToOne
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    private Integer quantity;
    private String notes;
}
