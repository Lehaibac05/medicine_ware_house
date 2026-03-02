package com.pharmacy.warehouse.model;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    private LocalDateTime orderDate;
    private String status;
    private Double subTotal;
    private Double discountAmount;
    private Double taxAmount;
    private Double totalAmount;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "order")
    @JsonIgnore
    private List<OrderItem> items;

    @OneToMany(mappedBy = "order")
    @JsonIgnore
    private List<Delivery> deliveries;

    @OneToMany(mappedBy = "order")
    @JsonIgnore
    private List<Payment> payments;

    public void updateStock() {
        // TODO: cập nhật tồn kho
    }

    public void checkThreshold() {
        // TODO: kiểm tra tồn kho thấp
    }

}
