package com.pharmacy.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUserImportRowResult {

    private int rowNumber;
    private String username;
    private String fullName;
    private String email;
    private String status;
    private String roleInput;
    private boolean valid;
    private List<String> errors;
    private Long createdUserId;
}
