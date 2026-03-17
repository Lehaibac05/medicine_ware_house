package com.pharmacy.warehouse.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "medicine_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicineRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long requestId;

    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    private LocalDate requiredDate;
    private String notes;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    private LocalDateTime createdDate;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<MedicineRequestItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = RequestStatus.PENDING;
        }
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }

    public enum RequestStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
