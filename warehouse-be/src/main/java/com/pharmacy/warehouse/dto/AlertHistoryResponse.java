package com.pharmacy.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertHistoryResponse {
    private Long historyId;
    private Long alertId;
    private String action;
    private String oldStatus;
    private String newStatus;
    private String comment;
    private LocalDateTime timestamp;
    private String username;
    private String userFullName;
}
