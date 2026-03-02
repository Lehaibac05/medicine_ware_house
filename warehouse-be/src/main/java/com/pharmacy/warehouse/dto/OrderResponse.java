package com.pharmacy.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long orderId;
    private LocalDateTime orderDate;
    private String status;
    private Double subTotal;
    private Double discountAmount;
    private Double taxAmount;
    private Double totalAmount;
    private Long userId;
    private String userName;
    private String userEmail;
    private List<OrderItemDTO> items;
}
