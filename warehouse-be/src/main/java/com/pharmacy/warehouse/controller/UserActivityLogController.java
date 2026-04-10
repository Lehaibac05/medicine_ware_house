package com.pharmacy.warehouse.controller;

import com.pharmacy.warehouse.dto.UserActivityLogResponse;
import com.pharmacy.warehouse.service.UserActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/activity-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserActivityLogController {

    private final UserActivityLogService userActivityLogService;

    @GetMapping
    public Page<UserActivityLogResponse> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action
    ) {
        return userActivityLogService.getLogs(page, size, action);
    }
}
