package com.pharmacy.warehouse.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierResponse {
    private Long supplierId;
    private String supplierName;
    private String contactPerson;
    private String phoneNumber;
    private String email;
    private String address;
    private String taxCode;
    private String qrBankTransferLink;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
