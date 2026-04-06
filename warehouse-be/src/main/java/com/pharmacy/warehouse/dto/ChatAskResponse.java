package com.pharmacy.warehouse.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ChatAskResponse {
    private String answer;
    private List<String> suggestions;
    private LocalDateTime timestamp;
}
