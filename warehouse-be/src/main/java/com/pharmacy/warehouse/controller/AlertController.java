package com.pharmacy.warehouse.controller;

import com.pharmacy.warehouse.dto.*;
import com.pharmacy.warehouse.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping({"/alerts", "/alerts/"})
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<List<AlertResponse>> getAllAlerts() {
        log.info("GET /alerts - Fetching all alerts");
        alertService.checkAndGenerateAlerts();
        List<AlertResponse> alerts = alertService.getAllAlerts();
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/active")
    public ResponseEntity<List<AlertResponse>> getActiveAlerts() {
        log.info("GET /alerts/active - Fetching active alerts");
        alertService.checkAndGenerateAlerts();
        List<AlertResponse> alerts = alertService.getActiveAlerts();
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertResponse> getAlertById(@PathVariable Long id) {
        log.info("GET /alerts/{} - Fetching alert", id);
        AlertResponse alert = alertService.getAlertById(id);
        return ResponseEntity.ok(alert);
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<AlertResponse>> getAlertsByType(@PathVariable String type) {
        log.info("GET /alerts/type/{} - Fetching alerts by type", type);
        List<AlertResponse> alerts = alertService.getAlertsByType(type);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/severity/{severity}")
    public ResponseEntity<List<AlertResponse>> getAlertsBySeverity(@PathVariable String severity) {
        log.info("GET /alerts/severity/{} - Fetching alerts by severity", severity);
        List<AlertResponse> alerts = alertService.getAlertsBySeverity(severity);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<AlertResponse>> getAlertsByStatus(@PathVariable String status) {
        log.info("GET /alerts/status/{} - Fetching alerts by status", status);
        List<AlertResponse> alerts = alertService.getAlertsByStatus(status);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/stats")
    public ResponseEntity<AlertStatsResponse> getAlertStats() {
        log.info("GET /alerts/stats - Fetching alert statistics");
        alertService.checkAndGenerateAlerts();
        AlertStatsResponse stats = alertService.getAlertStats();
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<AlertResponse> resolveAlert(
            @PathVariable Long id,
            @RequestBody(required = false) ResolveAlertRequest request,
            Authentication authentication) {
        
        String username = authentication != null ? authentication.getName() : "system";
        String comment = request != null ? request.getComment() : null;
        
        log.info("POST /alerts/{}/resolve - Resolving alert by user: {}", id, username);
        AlertResponse alert = alertService.resolveAlert(id, username, comment);
        return ResponseEntity.ok(alert);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AlertResponse> updateAlertStatus(
            @PathVariable Long id,
            @RequestParam String status,
            Authentication authentication) {
        
        String username = authentication != null ? authentication.getName() : "system";
        
        log.info("PATCH /alerts/{}/status - Updating status to: {}", id, status);
        AlertResponse alert = alertService.updateAlertStatus(id, status, username);
        return ResponseEntity.ok(alert);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<AlertHistoryResponse>> getAlertHistory(@PathVariable Long id) {
        log.info("GET /alerts/{}/history - Fetching alert history", id);
        List<AlertHistoryResponse> history = alertService.getAlertHistory(id);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> triggerAlertScan() {
        log.info("POST /alerts/scan - Manually triggering alert scan");
        int beforeActiveCount = alertService.getActiveAlerts().size();
        alertService.checkAndGenerateAlerts();
        int afterActiveCount = alertService.getActiveAlerts().size();
        int alertsGenerated = Math.max(afterActiveCount - beforeActiveCount, 0);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Alert scan completed successfully");
        response.put("alertsGenerated", alertsGenerated);
        
        return ResponseEntity.ok(response);
    }
}
