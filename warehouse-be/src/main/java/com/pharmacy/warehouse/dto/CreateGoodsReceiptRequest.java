package com.pharmacy.warehouse.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateGoodsReceiptRequest {
    private Long purchaseOrderId;
    private String qualityCheckNotes;
    private Boolean qualityPassed;
    private List<ReceivedItemInfo> receivedItems;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceivedItemInfo {
        private Long itemId;
        private Integer receivedQuantity;
        private LocalDate actualExpiryDate;
        private String lotNumber;
        private LocalDate manufactureDate;
    }
}
