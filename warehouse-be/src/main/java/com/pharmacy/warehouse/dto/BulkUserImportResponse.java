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
public class BulkUserImportResponse {

    private boolean dryRun;
    private int totalRows;
    private int validRows;
    private int invalidRows;
    private int createdRows;
    private String message;
    private List<BulkUserImportRowResult> rows;
}
