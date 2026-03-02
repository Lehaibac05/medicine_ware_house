package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.model.Alert;
import com.pharmacy.warehouse.model.Batch;
import com.pharmacy.warehouse.repository.AlertRepository;
import com.pharmacy.warehouse.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertScanService {

    private final AlertRepository alertRepository;
    private final BatchRepository batchRepository;

    // Thresholds
    private static final int EXPIRING_SOON_DAYS_CRITICAL = 7;   // < 7 days → CRITICAL
    private static final int EXPIRING_SOON_DAYS_HIGH = 30;      // < 30 days → HIGH
    private static final int EXPIRING_SOON_DAYS_MEDIUM = 60;    // < 60 days → MEDIUM
    
    private static final int LOW_STOCK_CRITICAL = 10;           // < 10 → CRITICAL
    private static final int LOW_STOCK_HIGH = 20;               // < 20 → HIGH
    private static final int LOW_STOCK_MEDIUM = 50;             // < 50 → MEDIUM

    /**
     * Scheduled task - runs every day at 6:00 AM
     * Cron: second, minute, hour, day, month, weekday
     */
    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void scheduledAlertScan() {
        log.info("=== Starting scheduled alert scan at {} ===", LocalDateTime.now());
        scanAllAlerts();
        log.info("=== Completed scheduled alert scan ===");
    }

    /**
     * Manual alert scan - can be triggered via API
     */
    @Transactional
    public int scanAllAlerts() {
        log.info("Scanning all batches for alerts...");
        
        List<Batch> allBatches = batchRepository.findAll();
        log.info("Found {} batches to scan", allBatches.size());
        
        int expiredCount = 0;
        int expiringCount = 0;
        int lowStockCount = 0;
        
        for (Batch batch : allBatches) {
            // Skip if batch has no expiry date or quantity
            if (batch.getExpiryDate() == null || batch.getQuantity() == null) {
                continue;
            }
            
            // Check for expired batches
            if (scanExpiredBatch(batch)) {
                expiredCount++;
            }
            
            // Check for expiring soon batches
            if (scanExpiringBatch(batch)) {
                expiringCount++;
            }
            
            // Check for low stock batches
            if (scanLowStockBatch(batch)) {
                lowStockCount++;
            }
        }
        
        // Auto-resolve alerts for batches that no longer meet criteria
        autoResolveAlerts();
        
        int totalGenerated = expiredCount + expiringCount + lowStockCount;
        log.info("Alert scan completed: {} expired, {} expiring, {} low stock (total: {})", 
                expiredCount, expiringCount, lowStockCount, totalGenerated);
        
        return totalGenerated;
    }

    private boolean scanExpiredBatch(Batch batch) {
        LocalDate today = LocalDate.now();
        
        if (batch.getExpiryDate().isBefore(today)) {
            // Check if alert already exists
            List<Alert> existingAlerts = alertRepository.findActiveAlertByBatchAndType(
                    batch.getBatchId(), "EXPIRED");
            
            if (existingAlerts.isEmpty()) {
                createAlert(
                        "EXPIRED",
                        "CRITICAL",
                        "Batch Expired",
                        String.format("Batch %s of %s has expired on %s", 
                                batch.getLotNumber(),
                                batch.getMedicine() != null ? batch.getMedicine().getName() : "Unknown",
                                batch.getExpiryDate()),
                        batch
                );
                return true;
            }
        }
        
        return false;
    }

    private boolean scanExpiringBatch(Batch batch) {
        LocalDate today = LocalDate.now();
        LocalDate expiryDate = batch.getExpiryDate();
        
        // Skip if already expired
        if (expiryDate.isBefore(today)) {
            return false;
        }
        
        long daysUntilExpiry = ChronoUnit.DAYS.between(today, expiryDate);
        
        String severity = null;
        if (daysUntilExpiry <= EXPIRING_SOON_DAYS_CRITICAL) {
            severity = "CRITICAL";
        } else if (daysUntilExpiry <= EXPIRING_SOON_DAYS_HIGH) {
            severity = "HIGH";
        } else if (daysUntilExpiry <= EXPIRING_SOON_DAYS_MEDIUM) {
            severity = "MEDIUM";
        }
        
        if (severity != null) {
            // Check if alert already exists
            List<Alert> existingAlerts = alertRepository.findActiveAlertByBatchAndType(
                    batch.getBatchId(), "EXPIRING_SOON");
            
            if (existingAlerts.isEmpty()) {
                createAlert(
                        "EXPIRING_SOON",
                        severity,
                        "Batch Expiring Soon",
                        String.format("Batch %s of %s will expire in %d days (%s)", 
                                batch.getLotNumber(),
                                batch.getMedicine() != null ? batch.getMedicine().getName() : "Unknown",
                                daysUntilExpiry,
                                expiryDate),
                        batch
                );
                return true;
            } else {
                // Update severity if changed
                Alert existingAlert = existingAlerts.get(0);
                if (!existingAlert.getSeverity().equals(severity)) {
                    existingAlert.setSeverity(severity);
                    existingAlert.setDescription(
                            String.format("Batch %s of %s will expire in %d days (%s)", 
                                    batch.getLotNumber(),
                                    batch.getMedicine() != null ? batch.getMedicine().getName() : "Unknown",
                                    daysUntilExpiry,
                                    expiryDate)
                    );
                    alertRepository.save(existingAlert);
                    log.info("Updated expiring alert severity for batch {}: {}", batch.getBatchId(), severity);
                }
            }
        }
        
        return false;
    }

    private boolean scanLowStockBatch(Batch batch) {
        int quantity = batch.getQuantity();
        
        String severity = null;
        if (quantity < LOW_STOCK_CRITICAL) {
            severity = "CRITICAL";
        } else if (quantity < LOW_STOCK_HIGH) {
            severity = "HIGH";
        } else if (quantity < LOW_STOCK_MEDIUM) {
            severity = "MEDIUM";
        }
        
        if (severity != null) {
            // Check if alert already exists
            List<Alert> existingAlerts = alertRepository.findActiveAlertByBatchAndType(
                    batch.getBatchId(), "LOW_STOCK");
            
            if (existingAlerts.isEmpty()) {
                createAlert(
                        "LOW_STOCK",
                        severity,
                        "Low Stock Alert",
                        String.format("Batch %s of %s has low stock: %d units remaining at %s", 
                                batch.getLotNumber(),
                                batch.getMedicine() != null ? batch.getMedicine().getName() : "Unknown",
                                quantity,
                                batch.getWarehouse() != null ? batch.getWarehouse().getName() : "Unknown warehouse"),
                        batch
                );
                return true;
            } else {
                // Update severity if changed
                Alert existingAlert = existingAlerts.get(0);
                if (!existingAlert.getSeverity().equals(severity)) {
                    existingAlert.setSeverity(severity);
                    existingAlert.setDescription(
                            String.format("Batch %s of %s has low stock: %d units remaining at %s", 
                                    batch.getLotNumber(),
                                    batch.getMedicine() != null ? batch.getMedicine().getName() : "Unknown",
                                    quantity,
                                    batch.getWarehouse() != null ? batch.getWarehouse().getName() : "Unknown warehouse")
                    );
                    alertRepository.save(existingAlert);
                    log.info("Updated low stock alert severity for batch {}: {}", batch.getBatchId(), severity);
                }
            }
        }
        
        return false;
    }

    private void createAlert(String alertType, String severity, String message, 
                            String description, Batch batch) {
        Alert alert = new Alert();
        alert.setAlertType(alertType);
        alert.setSeverity(severity);
        alert.setStatus("OPEN");
        alert.setMessage(message);
        alert.setDescription(description);
        alert.setCreatedAt(LocalDateTime.now());
        alert.setBatch(batch);
        
        if (batch.getMedicine() != null) {
            alert.setMedicine(batch.getMedicine());
        }
        if (batch.getWarehouse() != null) {
            alert.setWarehouse(batch.getWarehouse());
        }
        
        alertRepository.save(alert);
        log.info("Created {} alert for batch {}: {}", alertType, batch.getBatchId(), message);
    }

    /**
     * Auto-resolve alerts that no longer meet criteria
     * E.g., if stock was replenished, resolve LOW_STOCK alert
     */
    private void autoResolveAlerts() {
        List<Alert> activeAlerts = alertRepository.findActiveAlerts();
        int resolvedCount = 0;
        
        for (Alert alert : activeAlerts) {
            if (alert.getBatch() == null) {
                continue;
            }
            
            Batch batch = alert.getBatch();
            boolean shouldResolve = false;
            
            switch (alert.getAlertType()) {
                case "LOW_STOCK":
                    // Resolve if stock is back above threshold
                    if (batch.getQuantity() != null && batch.getQuantity() >= LOW_STOCK_MEDIUM) {
                        shouldResolve = true;
                    }
                    break;
                    
                case "EXPIRING_SOON":
                    // Resolve if batch was removed or already expired (will be handled by EXPIRED alert)
                    LocalDate today = LocalDate.now();
                    if (batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(today)) {
                        shouldResolve = true;
                    }
                    break;
            }
            
            if (shouldResolve) {
                alert.setStatus("RESOLVED");
                alert.setResolvedAt(LocalDateTime.now());
                alertRepository.save(alert);
                resolvedCount++;
                log.info("Auto-resolved {} alert for batch {}", alert.getAlertType(), batch.getBatchId());
            }
        }
        
        if (resolvedCount > 0) {
            log.info("Auto-resolved {} alerts", resolvedCount);
        }
    }
}
