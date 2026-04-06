package com.pharmacy.warehouse.dto;

import lombok.Data;

@Data
public class SettingsResponse {

    private GeneralSettings general;
    private InventorySettings inventory;
    private SecuritySettings security;
    private NotificationSettings notifications;
    private SystemSettings system;

    @Data
    public static class GeneralSettings {
        private String pharmacyName;
        private String contactEmail;
        private String address;
        private String phoneNumber;
    }

    @Data
    public static class InventorySettings {
        private Integer lowStockThreshold;
        private Integer expiryAlertDays;
        private Boolean enableAIForecast;
        private Boolean autoOrderEnabled;
        private Integer reorderPoint;
    }

    @Data
    public static class SecuritySettings {
        private String passwordPolicy;
        private Integer sessionTimeout;
        private Boolean twoFactorAuth;
        private Integer loginAttempts;
    }

    @Data
    public static class NotificationSettings {
        private Boolean emailNotifications;
        private Boolean lowStockAlert;
        private Boolean expiryAlert;
        private Boolean orderAlert;
    }

    @Data
    public static class SystemSettings {
        private Boolean darkMode;
        private String language;
        private String timezone;
        private String dateFormat;
    }
}
