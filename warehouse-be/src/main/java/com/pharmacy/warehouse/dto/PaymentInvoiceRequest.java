package com.pharmacy.warehouse.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInvoiceRequest {
    private BigDecimal amount;
    private String method;
    private String notes;
}
