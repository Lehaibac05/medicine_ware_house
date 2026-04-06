package com.pharmacy.warehouse.controller;

import com.pharmacy.warehouse.dto.SettingsResponse;
import com.pharmacy.warehouse.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    public SettingsResponse getSettings() {
        return settingsService.getSettings();
    }

    @PutMapping("/general")
    public void updateGeneral(@RequestBody SettingsResponse.GeneralSettings settings) {
        settingsService.updateGeneralSettings(settings);
    }

    @PutMapping("/inventory")
    public void updateInventory(@RequestBody SettingsResponse.InventorySettings settings) {
        settingsService.updateInventorySettings(settings);
    }

    @PutMapping("/security")
    public void updateSecurity(@RequestBody SettingsResponse.SecuritySettings settings) {
        settingsService.updateSecuritySettings(settings);
    }

    @PutMapping("/notifications")
    public void updateNotifications(@RequestBody SettingsResponse.NotificationSettings settings) {
        settingsService.updateNotificationSettings(settings);
    }

    @PutMapping("/system")
    public void updateSystem(@RequestBody SettingsResponse.SystemSettings settings) {
        settingsService.updateSystemSettings(settings);
    }
}
