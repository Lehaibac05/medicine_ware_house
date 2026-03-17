package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.dto.*;
import com.pharmacy.warehouse.model.*;
import com.pharmacy.warehouse.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
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

    private static final int EXPIRING_SOON_DAYS = 30;

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
                        "Auto-resolved: stock returned to normal level",
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
        LocalDate expiryThreshold = today.plusDays(EXPIRING_SOON_DAYS);
        
        List<Batch> batches = batchRepository.findAll();
        
        for (Batch batch : batches) {
            if (batch.getExpiryDate() != null) {
                LocalDate expiryDate = batch.getExpiryDate();
                
                if (expiryDate.isAfter(today) && expiryDate.isBefore(expiryThreshold)) {
                    List<Alert> existing = alertRepository.findActiveAlertByBatchAndType(
                            batch.getBatchId(), "EXPIRING_SOON");
                    
                    if (existing.isEmpty()) {
                        createExpiringSoonAlert(batch);
                    }
                }
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

        Alert alert = new Alert();
        alert.setAlertType("LOW_STOCK");
        alert.setSeverity(inventory.getTotalStock() <= 5 ? "CRITICAL" : "HIGH");
        alert.setStatus("OPEN");
        alert.setMessage("Low stock: " + medicine.getName());
        alert.setDescription("Stock level in warehouse '" + warehouse.getName() + "' is "
            + inventory.getTotalStock() + " units");
        alert.setCreatedAt(LocalDateTime.now());
        alert.setBatch(null);
        alert.setMedicine(medicine);
        alert.setWarehouse(warehouse);
        
        alertRepository.save(alert);
        createHistoryEntry(alert, "CREATED", null, "OPEN", "Auto-generated alert", null);
        
        log.info("Created LOW_STOCK alert for medicine {} in warehouse {}", medicine.getMedicineId(), warehouse.getWarehouseId());
    }

    private void createExpiringSoonAlert(Batch batch) {
        Alert alert = new Alert();
        alert.setAlertType("EXPIRING_SOON");
        alert.setSeverity("MEDIUM");
        alert.setStatus("OPEN");
        alert.setMessage("Expiring soon: " + batch.getMedicine().getName());
        alert.setDescription("Batch expires on " + batch.getExpiryDate());
        alert.setCreatedAt(LocalDateTime.now());
        alert.setBatch(batch);
        alert.setMedicine(batch.getMedicine());
        alert.setWarehouse(batch.getWarehouse());
        
        alertRepository.save(alert);
        createHistoryEntry(alert, "CREATED", null, "OPEN", "Auto-generated alert", null);
        
        log.info("Created EXPIRING_SOON alert for batch {}", batch.getBatchId());
    }

    private void createExpiredAlert(Batch batch) {
        Alert alert = new Alert();
        alert.setAlertType("EXPIRED");
        alert.setSeverity("CRITICAL");
        alert.setStatus("OPEN");
        alert.setMessage("Expired: " + batch.getMedicine().getName());
        alert.setDescription("Batch expired on " + batch.getExpiryDate());
        alert.setCreatedAt(LocalDateTime.now());
        alert.setBatch(batch);
        alert.setMedicine(batch.getMedicine());
        alert.setWarehouse(batch.getWarehouse());
        
        alertRepository.save(alert);
        createHistoryEntry(alert, "CREATED", null, "OPEN", "Auto-generated alert", null);
        
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
