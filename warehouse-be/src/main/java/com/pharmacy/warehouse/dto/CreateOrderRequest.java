package com.pharmacy.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    private Long userId;
    private Double discountAmount;
    private Double taxAmount;
    private List<OrderItemRequest> items;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        private Long batchId;
        private Integer quantity;
        private Double unitPrice;
        private Double discount;
        private Double tax;
    }
}
