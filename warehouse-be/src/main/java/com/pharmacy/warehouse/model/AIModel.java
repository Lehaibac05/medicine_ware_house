package com.pharmacy.warehouse.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_model")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long modelId;

    private String modelName;
    private String version;
    private Double accuracy;

    public void predictDemand() {
    }

    public void detectAnomaly() {
    }
}
