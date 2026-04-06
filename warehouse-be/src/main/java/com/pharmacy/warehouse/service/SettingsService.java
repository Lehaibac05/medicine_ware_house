package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.dto.SettingsResponse;
import com.pharmacy.warehouse.model.SystemConfiguration;
import com.pharmacy.warehouse.repository.SystemConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private static final String KEY_GENERAL_PHARMACY_NAME = "general.pharmacyName";
    private static final String KEY_GENERAL_CONTACT_EMAIL = "general.contactEmail";
    private static final String KEY_GENERAL_ADDRESS = "general.address";
    private static final String KEY_GENERAL_PHONE_NUMBER = "general.phoneNumber";

    private static final String KEY_INVENTORY_LOW_STOCK_THRESHOLD = "inventory.lowStockThreshold";
    private static final String KEY_INVENTORY_EXPIRY_ALERT_DAYS = "inventory.expiryAlertDays";
    private static final String KEY_INVENTORY_ENABLE_AI_FORECAST = "inventory.enableAIForecast";
    private static final String KEY_INVENTORY_AUTO_ORDER_ENABLED = "inventory.autoOrderEnabled";
    private static final String KEY_INVENTORY_REORDER_POINT = "inventory.reorderPoint";

    private static final String KEY_SECURITY_PASSWORD_POLICY = "security.passwordPolicy";
    private static final String KEY_SECURITY_SESSION_TIMEOUT = "security.sessionTimeout";
    private static final String KEY_SECURITY_TWO_FACTOR_AUTH = "security.twoFactorAuth";
    private static final String KEY_SECURITY_LOGIN_ATTEMPTS = "security.loginAttempts";

    private static final String KEY_NOTIFICATIONS_EMAIL = "notifications.emailNotifications";
    private static final String KEY_NOTIFICATIONS_LOW_STOCK = "notifications.lowStockAlert";
    private static final String KEY_NOTIFICATIONS_EXPIRY = "notifications.expiryAlert";
    private static final String KEY_NOTIFICATIONS_ORDER = "notifications.orderAlert";

    private static final String KEY_SYSTEM_DARK_MODE = "system.darkMode";
    private static final String KEY_SYSTEM_LANGUAGE = "system.language";
    private static final String KEY_SYSTEM_TIMEZONE = "system.timezone";
    private static final String KEY_SYSTEM_DATE_FORMAT = "system.dateFormat";

    private final SystemConfigurationRepository systemConfigurationRepository;

    @Transactional(readOnly = true)
    public SettingsResponse getSettings() {
        SettingsResponse response = new SettingsResponse();

        SettingsResponse.GeneralSettings general = new SettingsResponse.GeneralSettings();
        general.setPharmacyName(readString(KEY_GENERAL_PHARMACY_NAME, "Main Street Pharmacy"));
        general.setContactEmail(readString(KEY_GENERAL_CONTACT_EMAIL, "admin@pharmacy.com"));
        general.setAddress(readString(KEY_GENERAL_ADDRESS, "123 Health Ave, Medical District, City State"));
        general.setPhoneNumber(readString(KEY_GENERAL_PHONE_NUMBER, "+1 (555) 012-3456"));
        response.setGeneral(general);

        SettingsResponse.InventorySettings inventory = new SettingsResponse.InventorySettings();
        inventory.setLowStockThreshold(readInt(KEY_INVENTORY_LOW_STOCK_THRESHOLD, 50));
        inventory.setExpiryAlertDays(readInt(KEY_INVENTORY_EXPIRY_ALERT_DAYS, 30));
        inventory.setEnableAIForecast(readBoolean(KEY_INVENTORY_ENABLE_AI_FORECAST, true));
        inventory.setAutoOrderEnabled(readBoolean(KEY_INVENTORY_AUTO_ORDER_ENABLED, false));
        inventory.setReorderPoint(readInt(KEY_INVENTORY_REORDER_POINT, 10));
        response.setInventory(inventory);

        SettingsResponse.SecuritySettings security = new SettingsResponse.SecuritySettings();
        security.setPasswordPolicy(readString(KEY_SECURITY_PASSWORD_POLICY, "medium"));
        security.setSessionTimeout(readInt(KEY_SECURITY_SESSION_TIMEOUT, 15));
        security.setTwoFactorAuth(readBoolean(KEY_SECURITY_TWO_FACTOR_AUTH, false));
        security.setLoginAttempts(readInt(KEY_SECURITY_LOGIN_ATTEMPTS, 5));
        response.setSecurity(security);

        SettingsResponse.NotificationSettings notifications = new SettingsResponse.NotificationSettings();
        notifications.setEmailNotifications(readBoolean(KEY_NOTIFICATIONS_EMAIL, true));
        notifications.setLowStockAlert(readBoolean(KEY_NOTIFICATIONS_LOW_STOCK, true));
        notifications.setExpiryAlert(readBoolean(KEY_NOTIFICATIONS_EXPIRY, true));
        notifications.setOrderAlert(readBoolean(KEY_NOTIFICATIONS_ORDER, true));
        response.setNotifications(notifications);

        SettingsResponse.SystemSettings system = new SettingsResponse.SystemSettings();
        system.setDarkMode(readBoolean(KEY_SYSTEM_DARK_MODE, false));
        system.setLanguage(readString(KEY_SYSTEM_LANGUAGE, "en"));
        system.setTimezone(readString(KEY_SYSTEM_TIMEZONE, "UTC+7"));
        system.setDateFormat(readString(KEY_SYSTEM_DATE_FORMAT, "DD/MM/YYYY"));
        response.setSystem(system);

        return response;
    }

    @Transactional
    public void updateGeneralSettings(SettingsResponse.GeneralSettings settings) {
        if (settings == null) {
            return;
        }
        writeString(KEY_GENERAL_PHARMACY_NAME, settings.getPharmacyName());
        writeString(KEY_GENERAL_CONTACT_EMAIL, settings.getContactEmail());
        writeString(KEY_GENERAL_ADDRESS, settings.getAddress());
        writeString(KEY_GENERAL_PHONE_NUMBER, settings.getPhoneNumber());
    }

    @Transactional
    public void updateInventorySettings(SettingsResponse.InventorySettings settings) {
        if (settings == null) {
            return;
        }
        writeInt(KEY_INVENTORY_LOW_STOCK_THRESHOLD, settings.getLowStockThreshold(), 50);
        int normalizedExpiryAlertDays = settings.getExpiryAlertDays() == null
                ? 30
                : Math.max(1, settings.getExpiryAlertDays());
        upsert(KEY_INVENTORY_EXPIRY_ALERT_DAYS, String.valueOf(normalizedExpiryAlertDays));
        writeBoolean(KEY_INVENTORY_ENABLE_AI_FORECAST, settings.getEnableAIForecast(), true);
        writeBoolean(KEY_INVENTORY_AUTO_ORDER_ENABLED, settings.getAutoOrderEnabled(), false);
        writeInt(KEY_INVENTORY_REORDER_POINT, settings.getReorderPoint(), 10);
    }

    @Transactional
    public void updateSecuritySettings(SettingsResponse.SecuritySettings settings) {
        if (settings == null) {
            return;
        }
        writeString(KEY_SECURITY_PASSWORD_POLICY, settings.getPasswordPolicy());
        writeInt(KEY_SECURITY_SESSION_TIMEOUT, settings.getSessionTimeout(), 15);
        writeBoolean(KEY_SECURITY_TWO_FACTOR_AUTH, settings.getTwoFactorAuth(), false);
        writeInt(KEY_SECURITY_LOGIN_ATTEMPTS, settings.getLoginAttempts(), 5);
    }

    @Transactional
    public void updateNotificationSettings(SettingsResponse.NotificationSettings settings) {
        if (settings == null) {
            return;
        }
        writeBoolean(KEY_NOTIFICATIONS_EMAIL, settings.getEmailNotifications(), true);
        writeBoolean(KEY_NOTIFICATIONS_LOW_STOCK, settings.getLowStockAlert(), true);
        writeBoolean(KEY_NOTIFICATIONS_EXPIRY, settings.getExpiryAlert(), true);
        writeBoolean(KEY_NOTIFICATIONS_ORDER, settings.getOrderAlert(), true);
    }

    @Transactional
    public void updateSystemSettings(SettingsResponse.SystemSettings settings) {
        if (settings == null) {
            return;
        }
        writeBoolean(KEY_SYSTEM_DARK_MODE, settings.getDarkMode(), false);
        writeString(KEY_SYSTEM_LANGUAGE, settings.getLanguage());
        writeString(KEY_SYSTEM_TIMEZONE, settings.getTimezone());
        writeString(KEY_SYSTEM_DATE_FORMAT, settings.getDateFormat());
    }

    @Transactional(readOnly = true)
    public int getDefaultReorderLevel() {
        return readInt(KEY_INVENTORY_REORDER_POINT, 10);
    }

    @Transactional(readOnly = true)
    public int getExpiryAlertDays() {
        return readInt(KEY_INVENTORY_EXPIRY_ALERT_DAYS, 30);
    }

    @Transactional(readOnly = true)
    public String getContactEmail() {
        return readString(KEY_GENERAL_CONTACT_EMAIL, "admin@pharmacy.com");
    }

    @Transactional(readOnly = true)
    public boolean isEmailNotificationsEnabled() {
        return readBoolean(KEY_NOTIFICATIONS_EMAIL, true);
    }

    @Transactional(readOnly = true)
    public boolean isLowStockEmailEnabled() {
        return readBoolean(KEY_NOTIFICATIONS_LOW_STOCK, true);
    }

    @Transactional(readOnly = true)
    public boolean isExpiryEmailEnabled() {
        return readBoolean(KEY_NOTIFICATIONS_EXPIRY, true);
    }

    private String readString(String key, String defaultValue) {
        return systemConfigurationRepository.findByParameterName(key)
                .map(SystemConfiguration::getConfigValue)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElse(defaultValue);
    }

    private int readInt(String key, int defaultValue) {
        String value = readString(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private boolean readBoolean(String key, boolean defaultValue) {
        String value = readString(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    private void writeString(String key, String value) {
        String normalized = value == null ? "" : value.trim();
        upsert(key, normalized);
    }

    private void writeInt(String key, Integer value, int fallback) {
        int normalized = value == null ? fallback : Math.max(0, value);
        upsert(key, String.valueOf(normalized));
    }

    private void writeBoolean(String key, Boolean value, boolean fallback) {
        boolean normalized = value == null ? fallback : value;
        upsert(key, String.valueOf(normalized));
    }

    private void upsert(String key, String value) {
        SystemConfiguration configuration = systemConfigurationRepository
                .findByParameterName(key)
                .orElseGet(SystemConfiguration::new);

        configuration.setParameterName(key);
        configuration.setConfigValue(value);
        configuration.setUpdatedDate(LocalDateTime.now());

        systemConfigurationRepository.save(configuration);
    }
}
