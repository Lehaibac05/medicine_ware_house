package com.pharmacy.warehouse.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "payment")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    private LocalDateTime paymentDate;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    private String method;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(length = 100)
    private String transactionReference;

    private String notes;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "supplier_invoice_id")
    private SupplierInvoice supplierInvoice;

    public enum PaymentStatus {
        COMPLETED,
        FAILED,
        CANCELLED,
        REVERSED
    }
}
