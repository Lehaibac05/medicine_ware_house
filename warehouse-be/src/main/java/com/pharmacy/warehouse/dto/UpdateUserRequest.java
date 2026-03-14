package com.pharmacy.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    private String fullName;
    private String email;
    /**
     * Expected values, e.g. ACTIVE / INACTIVE.
     */
    private String status;
    private Long roleId;
    /**
     * Optional new password. If null or blank, password will not be changed.
     */
    private String password;
}
