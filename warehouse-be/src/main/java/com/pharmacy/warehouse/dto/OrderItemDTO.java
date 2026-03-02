package com.pharmacy.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private Long orderItemId;
    private Long batchId;
    private String medicineName;
    private String lotNumber;
    private Integer quantity;
    private Double unitPrice;
    private Double discount;
    private Double tax;
    private Double totalPrice;
}
