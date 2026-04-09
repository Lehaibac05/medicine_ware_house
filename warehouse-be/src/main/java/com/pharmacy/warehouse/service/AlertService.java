package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.dto.*;
import com.pharmacy.warehouse.model.*;
import com.pharmacy.warehouse.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final MedicineRepository medicineRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryService inventoryService;
    private final SettingsService settingsService;
    private final EmailService emailService;

    private static final double CRITICAL_RATIO = 0.5;
    private static final double HIGH_RATIO = 0.8;
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_WAREHOUSE_MANAGER = "WAREHOUSE_MANAGER";
    private static final String ROLE_WAREHOUSE_STAFF = "WAREHOUSE_STAFF";
    private static final String ROLE_ACCOUNTANT = "ACCOUNTANT";

    @Transactional(readOnly = true)
    public List<AlertResponse> getAllAlerts() {
        return alertRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getActiveAlerts() {
        return alertRepository.findActiveAlerts().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AlertResponse getAlertById(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + id));
        return convertToResponse(alert);
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getAlertsByType(String type) {
        return alertRepository.findByAlertType(type).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getAlertsBySeverity(String severity) {
        return alertRepository.findBySeverity(severity).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getAlertsByStatus(String status) {
        return alertRepository.findByStatus(status).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AlertStatsResponse getAlertStats() {
        AlertStatsResponse stats = new AlertStatsResponse();
        
        stats.setLowStockCount(alertRepository.countActiveAlertsByType("LOW_STOCK"));
        stats.setExpiringSoonCount(alertRepository.countActiveAlertsByType("EXPIRING_SOON"));
        stats.setExpiredCount(alertRepository.countActiveAlertsByType("EXPIRED"));
        stats.setSystemWarningsCount(alertRepository.countActiveAlertsByType("SYSTEM"));
        
        Long total = stats.getLowStockCount() + stats.getExpiringSoonCount() + 
                     stats.getExpiredCount() + stats.getSystemWarningsCount();
        stats.setTotalActiveAlerts(total);
        
        return stats;
    }

    @Transactional
    public AlertResponse resolveAlert(Long alertId, String username, String comment) {
        log.info("Resolving alert {} by user: {}", alertId, username);
        
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + alertId));
        
        User user = userRepository.findByUsername(username)
                .orElse(null);

        if ("EXPIRED".equalsIgnoreCase(alert.getAlertType())) {
            if (!canResolveExpiredAlert(user)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền đóng cảnh báo đã hết hạn");
            }
            if (!StringUtils.hasText(comment) || comment.trim().length() < 10) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cảnh báo đã hết hạn yêu cầu ghi chú xử lý tối thiểu 10 ký tự");
            }
        }
        
        String oldStatus = alert.getStatus();
        alert.setStatus("RESOLVED");
        alert.setResolvedAt(LocalDateTime.now());
        alert.setResolvedBy(user);
        
        alert = alertRepository.save(alert);
        
        // Create history entry
        createHistoryEntry(alert, "RESOLVED", oldStatus, "RESOLVED", comment, user);
        
        log.info("Alert {} resolved successfully", alertId);
        return convertToResponse(alert);
    }

    @Transactional
    public AlertResponse updateAlertStatus(Long alertId, String newStatus, String username) {
        log.info("Updating alert {} status to: {}", alertId, newStatus);
        
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + alertId));
        
        User user = userRepository.findByUsername(username).orElse(null);

        if ("EXPIRED".equalsIgnoreCase(alert.getAlertType()) && !canResolveExpiredAlert(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền cập nhật cảnh báo đã hết hạn");
        }

        if ("RESOLVED".equalsIgnoreCase(newStatus) && !canResolveExpiredAlert(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền đóng cảnh báo");
        }
        
        String oldStatus = alert.getStatus();
        alert.setStatus(newStatus);
        
        if ("RESOLVED".equals(newStatus)) {
            alert.setResolvedAt(LocalDateTime.now());
            alert.setResolvedBy(user);
        }
        
        alert = alertRepository.save(alert);
        
        createHistoryEntry(alert, "STATUS_CHANGED", oldStatus, newStatus, null, user);
        
        return convertToResponse(alert);
    }

    @Transactional(readOnly = true)
    public List<AlertHistoryResponse> getAlertHistory(Long alertId) {
        return alertHistoryRepository.findHistoryByAlertId(alertId).stream()
                .map(this::convertHistoryToResponse)
                .collect(Collectors.toList());
    }

    // ==================== AUTO ALERT GENERATION ====================
    
    /**
     * Scheduled task to check for alerts every hour
     */
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    @Transactional
    public void checkAndGenerateAlerts() {
        log.info("Starting automated alert check...");
        
        checkLowStockAlerts();
        checkExpiringBatches();
        checkExpiredBatches();
        
        log.info("Automated alert check completed");
    }

    @Transactional
    public void checkLowStockAlerts() {
        List<InventoryResponse> lowStockInventory = inventoryService.getLowStockInventory();
        Set<String> lowStockKeys = new HashSet<>();

        for (InventoryResponse inventory : lowStockInventory) {
            lowStockKeys.add(buildMedicineWarehouseKey(inventory.getMedicineId(), inventory.getWarehouseId()));

            List<Alert> existing = alertRepository.findActiveAlertByMedicineWarehouseAndType(
                    inventory.getMedicineId(), inventory.getWarehouseId(), "LOW_STOCK");

            if (existing.isEmpty()) {
                createLowStockAlert(inventory);
            }
        }

        autoResolveRecoveredLowStockAlerts(lowStockKeys);
    }

    private void autoResolveRecoveredLowStockAlerts(Set<String> lowStockKeys) {
        List<Alert> lowStockAlerts = alertRepository.findByAlertType("LOW_STOCK");

        for (Alert alert : lowStockAlerts) {
            boolean activeAlert = "OPEN".equals(alert.getStatus()) || "IN_PROGRESS".equals(alert.getStatus());
            if (!activeAlert || alert.getMedicine() == null || alert.getWarehouse() == null) {
                continue;
            }

            String key = buildMedicineWarehouseKey(
                    alert.getMedicine().getMedicineId(),
                    alert.getWarehouse().getWarehouseId());

            if (!lowStockKeys.contains(key)) {
                String oldStatus = alert.getStatus();
                alert.setStatus("RESOLVED");
                alert.setResolvedAt(LocalDateTime.now());
                alert.setResolvedBy(null);

                Alert updated = alertRepository.save(alert);
                createHistoryEntry(
                        updated,
                        "AUTO_RESOLVED",
                        oldStatus,
                        "RESOLVED",
                    "Tự động xử lý: tồn kho đã quay về mức an toàn",
                        null);

                log.info(
                        "Auto-resolved LOW_STOCK alert {} for medicine {} in warehouse {}",
                        updated.getAlertId(),
                        updated.getMedicine().getMedicineId(),
                        updated.getWarehouse().getWarehouseId());
            }
        }
    }

    private String buildMedicineWarehouseKey(Long medicineId, Long warehouseId) {
        return medicineId + "-" + warehouseId;
    }

    @Transactional
    public void checkExpiringBatches() {
        LocalDate today = LocalDate.now();
        LocalDate expiryThreshold = today.plusDays(settingsService.getExpiryAlertDays());
        Set<Long> expiringBatchIds = new HashSet<>();

        List<Batch> batches = batchRepository.findAll();
        
        for (Batch batch : batches) {
            if (batch.getExpiryDate() != null && batch.getQuantity() != null && batch.getQuantity() > 0) {
                LocalDate expiryDate = batch.getExpiryDate();
                
                if (!expiryDate.isBefore(today) && !expiryDate.isAfter(expiryThreshold)) {
                    expiringBatchIds.add(batch.getBatchId());
                    List<Alert> existing = alertRepository.findActiveAlertByBatchAndType(
                            batch.getBatchId(), "EXPIRING_SOON");
                    
                    if (existing.isEmpty()) {
                        createExpiringSoonAlert(batch);
                    }
                }
            }
        }

        autoResolveRecoveredExpiringSoonAlerts(expiringBatchIds);
    }

    private void autoResolveRecoveredExpiringSoonAlerts(Set<Long> expiringBatchIds) {
        List<Alert> expiringAlerts = alertRepository.findByAlertType("EXPIRING_SOON");

        for (Alert alert : expiringAlerts) {
            boolean activeAlert = "OPEN".equals(alert.getStatus()) || "IN_PROGRESS".equals(alert.getStatus());
            if (!activeAlert || alert.getBatch() == null) {
                continue;
            }

            if (!expiringBatchIds.contains(alert.getBatch().getBatchId())) {
                String oldStatus = alert.getStatus();
                alert.setStatus("RESOLVED");
                alert.setResolvedAt(LocalDateTime.now());
                alert.setResolvedBy(null);

                Alert updated = alertRepository.save(alert);
                createHistoryEntry(
                        updated,
                        "AUTO_RESOLVED",
                        oldStatus,
                        "RESOLVED",
                    "Tự động xử lý: lô thuốc không còn trong ngưỡng sắp hết hạn",
                        null);

                log.info("Auto-resolved EXPIRING_SOON alert {} for batch {}", updated.getAlertId(), updated.getBatch().getBatchId());
            }
        }
    }

    @Transactional
    public void checkExpiredBatches() {
        LocalDate today = LocalDate.now();
        List<Batch> batches = batchRepository.findAll();
        
        for (Batch batch : batches) {
            if (batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(today)) {
                List<Alert> existing = alertRepository.findActiveAlertByBatchAndType(
                        batch.getBatchId(), "EXPIRED");
                
                if (existing.isEmpty()) {
                    createExpiredAlert(batch);
                }
            }
        }
    }

        private void createLowStockAlert(InventoryResponse inventory) {
        Medicine medicine = medicineRepository.findById(inventory.getMedicineId())
            .orElseThrow(() -> new RuntimeException("Medicine not found with id: " + inventory.getMedicineId()));
        Warehouse warehouse = warehouseRepository.findById(inventory.getWarehouseId())
            .orElseThrow(() -> new RuntimeException("Warehouse not found with id: " + inventory.getWarehouseId()));

        int reorderLevel = resolveReorderLevel(medicine.getReorderLevel());
        String severity = determineLowStockSeverity(inventory.getTotalStock(), reorderLevel);

        Alert alert = new Alert();
        alert.setAlertType("LOW_STOCK");
        alert.setSeverity(severity);
        alert.setStatus("OPEN");
        alert.setMessage("Tồn kho thấp: " + medicine.getName());
        alert.setDescription("Số lượng tồn tại kho '" + warehouse.getName() + "' còn "
            + inventory.getTotalStock() + " đơn vị");
        alert.setCreatedAt(LocalDateTime.now());
        alert.setBatch(null);
        alert.setMedicine(medicine);
        alert.setWarehouse(warehouse);
        
        alertRepository.save(alert);
        createHistoryEntry(alert, "CREATED", null, "OPEN", "Cảnh báo được tạo tự động", null);
        sendAlertNotificationEmail(alert);
        
        log.info("Created LOW_STOCK alert for medicine {} in warehouse {}", medicine.getMedicineId(), warehouse.getWarehouseId());
    }

    private int resolveReorderLevel(Integer reorderLevel) {
        if (reorderLevel == null || reorderLevel <= 0) {
            return settingsService.getDefaultReorderLevel();
        }
        return reorderLevel;
    }

    private String determineLowStockSeverity(Long totalStock, int reorderLevel) {
        long stock = totalStock == null ? 0L : totalStock;
        double criticalThreshold = reorderLevel * CRITICAL_RATIO;
        double highThreshold = reorderLevel * HIGH_RATIO;

        if (stock <= criticalThreshold) {
            return "CRITICAL";
        }
        if (stock <= highThreshold) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    private void createExpiringSoonAlert(Batch batch) {
        Alert alert = new Alert();
        alert.setAlertType("EXPIRING_SOON");
        alert.setSeverity("MEDIUM");
        alert.setStatus("OPEN");
        alert.setMessage("Sắp hết hạn: " + batch.getMedicine().getName());
        alert.setDescription("Lô thuốc sẽ hết hạn vào ngày " + batch.getExpiryDate());
        alert.setCreatedAt(LocalDateTime.now());
        alert.setBatch(batch);
        alert.setMedicine(batch.getMedicine());
        alert.setWarehouse(batch.getWarehouse());
        
        alertRepository.save(alert);
        createHistoryEntry(alert, "CREATED", null, "OPEN", "Cảnh báo được tạo tự động", null);
        sendAlertNotificationEmail(alert);
        
        log.info("Created EXPIRING_SOON alert for batch {}", batch.getBatchId());
    }

    private void createExpiredAlert(Batch batch) {
        Alert alert = new Alert();
        alert.setAlertType("EXPIRED");
        alert.setSeverity("CRITICAL");
        alert.setStatus("OPEN");
        alert.setMessage("Đã hết hạn: " + batch.getMedicine().getName());
        alert.setDescription("Lô thuốc đã hết hạn vào ngày " + batch.getExpiryDate());
        alert.setCreatedAt(LocalDateTime.now());
        alert.setBatch(batch);
        alert.setMedicine(batch.getMedicine());
        alert.setWarehouse(batch.getWarehouse());
        
        alertRepository.save(alert);
        createHistoryEntry(alert, "CREATED", null, "OPEN", "Cảnh báo được tạo tự động", null);
        sendAlertNotificationEmail(alert);
        
        log.info("Created EXPIRED alert for batch {}", batch.getBatchId());
    }

    private void createHistoryEntry(Alert alert, String action, String oldStatus, 
                                   String newStatus, String comment, User user) {
        AlertHistory history = new AlertHistory();
        history.setAlert(alert);
        history.setAction(action);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setComment(comment);
        history.setTimestamp(LocalDateTime.now());
        history.setUser(user);
        
        alertHistoryRepository.save(history);
    }

    private void sendAlertNotificationEmail(Alert alert) {
        if (!settingsService.isEmailNotificationsEnabled() || !isAlertTypeEmailEnabled(alert.getAlertType())) {
            return;
        }

        Set<String> recipients = resolveRecipientsByAlertType(alert.getAlertType());
        if (recipients.isEmpty()) {
            return;
        }

        String subject = "[Cảnh báo kho thuốc] "
            + toVietnameseAlertType(alert.getAlertType())
            + " - "
            + toVietnameseSeverity(alert.getSeverity());
        String body = "Loại cảnh báo: " + toVietnameseAlertType(alert.getAlertType()) + "\n"
            + "Mức độ: " + toVietnameseSeverity(alert.getSeverity()) + "\n"
            + "Nội dung: " + safe(alert.getMessage()) + "\n"
            + "Chi tiết: " + safe(alert.getDescription()) + "\n"
            + "Thời gian tạo: " + alert.getCreatedAt() + "\n"
            + "Trạng thái: " + toVietnameseStatus(alert.getStatus());

        for (String recipient : recipients) {
            emailService.sendSimpleMessage(recipient, subject, body);
        }
    }

    private Set<String> resolveRecipientsByAlertType(String alertType) {
        Set<String> roleNames = new LinkedHashSet<>();
        roleNames.add(ROLE_ADMIN);
        roleNames.add(ROLE_WAREHOUSE_MANAGER);
        roleNames.add(ROLE_WAREHOUSE_STAFF);

        // Accountant receives only expired alerts by default to reduce noise.
        if ("EXPIRED".equals(alertType)) {
            roleNames.add(ROLE_ACCOUNTANT);
        }

        List<User> users = userRepository.findByStatusIgnoreCaseAndRole_RoleNameIn("ACTIVE", roleNames);
        return users.stream()
                .map(User::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isAlertTypeEmailEnabled(String alertType) {
        if ("LOW_STOCK".equals(alertType)) {
            return settingsService.isLowStockEmailEnabled();
        }
        if ("EXPIRING_SOON".equals(alertType) || "EXPIRED".equals(alertType)) {
            return settingsService.isExpiryEmailEnabled();
        }
        return true;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String toVietnameseAlertType(String alertType) {
        return switch (alertType) {
            case "LOW_STOCK" -> "Tồn kho thấp";
            case "EXPIRING_SOON" -> "Sắp hết hạn";
            case "EXPIRED" -> "Đã hết hạn";
            case "SYSTEM" -> "Hệ thống";
            default -> safe(alertType);
        };
    }

    private String toVietnameseSeverity(String severity) {
        return switch (severity) {
            case "CRITICAL" -> "Nghiêm trọng";
            case "HIGH" -> "Cao";
            case "MEDIUM" -> "Trung bình";
            case "LOW" -> "Thấp";
            default -> safe(severity);
        };
    }

    private String toVietnameseStatus(String status) {
        return switch (status) {
            case "OPEN" -> "Mở";
            case "IN_PROGRESS" -> "Đang xử lý";
            case "RESOLVED" -> "Đã xử lý";
            default -> safe(status);
        };
    }

    private boolean canResolveExpiredAlert(User user) {
        if (user == null || user.getRole() == null || user.getRole().getRoleName() == null) {
            return false;
        }

        String roleName = user.getRole().getRoleName().trim().toUpperCase();
        if (roleName.startsWith("ROLE_")) {
            roleName = roleName.substring(5);
        }

        return ROLE_ADMIN.equals(roleName) || ROLE_WAREHOUSE_MANAGER.equals(roleName);
    }

    private AlertResponse convertToResponse(Alert alert) {
        AlertResponse response = new AlertResponse();
        response.setAlertId(alert.getAlertId());
        response.setAlertType(alert.getAlertType());
        response.setSeverity(alert.getSeverity());
        response.setStatus(alert.getStatus());
        response.setMessage(alert.getMessage());
        response.setDescription(alert.getDescription());
        response.setCreatedAt(alert.getCreatedAt());
        response.setResolvedAt(alert.getResolvedAt());
        
        if (alert.getBatch() != null) {
            response.setBatchId(alert.getBatch().getBatchId());
            response.setLotNumber(alert.getBatch().getLotNumber());
        }
        
        if (alert.getMedicine() != null) {
            response.setMedicineId(alert.getMedicine().getMedicineId());
            response.setMedicineName(alert.getMedicine().getName());
        }
        
        if (alert.getWarehouse() != null) {
            response.setWarehouseId(alert.getWarehouse().getWarehouseId());
            response.setWarehouseName(alert.getWarehouse().getName());
        }
        
        if (alert.getResolvedBy() != null) {
            response.setResolvedByUserId(alert.getResolvedBy().getUserId());
            response.setResolvedByUsername(alert.getResolvedBy().getUsername());
        }
        
        return response;
    }

    private AlertHistoryResponse convertHistoryToResponse(AlertHistory history) {
        AlertHistoryResponse response = new AlertHistoryResponse();
        response.setHistoryId(history.getHistoryId());
        response.setAlertId(history.getAlert().getAlertId());
        response.setAction(history.getAction());
        response.setOldStatus(history.getOldStatus());
        response.setNewStatus(history.getNewStatus());
        response.setComment(history.getComment());
        response.setTimestamp(history.getTimestamp());
        
        if (history.getUser() != null) {
            response.setUsername(history.getUser().getUsername());
            response.setUserFullName(history.getUser().getFullName());
        }
        
        return response;
    }
}
