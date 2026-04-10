package com.pharmacy.warehouse.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserActivityLogResponse {
    private Long logId;
    private String action;
    private String targetObject;
    private LocalDateTime timestamp;
    private String ipAddress;
    private Long userId;
    private String username;
    private String fullName;
}
