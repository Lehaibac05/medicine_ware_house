package com.pharmacy.warehouse.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "medicine")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long medicineId;

    private String name;
    private String manufacturer;
    private String storageCondition;
    private String description;

    @OneToMany(mappedBy = "medicine")
    @JsonIgnore
    private List<Batch> batches;
}

