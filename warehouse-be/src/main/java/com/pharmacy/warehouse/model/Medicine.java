package com.pharmacy.warehouse.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "Medicine name is required")
    @Size(max = 255, message = "Medicine name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Manufacturer is required")
    @Size(max = 255, message = "Manufacturer must not exceed 255 characters")
    private String manufacturer;

    @NotBlank(message = "Storage condition is required")
    @Size(max = 255, message = "Storage condition must not exceed 255 characters")
    private String storageCondition;

    @NotBlank(message = "Description is required")
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    private Integer reorderLevel;

    @OneToMany(mappedBy = "medicine")
    @JsonIgnore
    private List<Batch> batches;

    @PrePersist
    public void onCreate() {
        if (reorderLevel == null || reorderLevel < 0) {
            reorderLevel = 10;
        }
    }
}

