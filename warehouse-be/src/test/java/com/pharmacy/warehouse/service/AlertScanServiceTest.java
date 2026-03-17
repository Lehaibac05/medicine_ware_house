package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.model.Alert;
import com.pharmacy.warehouse.model.Batch;
import com.pharmacy.warehouse.model.Medicine;
import com.pharmacy.warehouse.model.Warehouse;
import com.pharmacy.warehouse.repository.AlertRepository;
import com.pharmacy.warehouse.repository.BatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AlertScanServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private BatchRepository batchRepository;

    @InjectMocks
    private AlertScanService alertScanService;

    private Batch testBatch;
    private Medicine testMedicine;
    private Warehouse testWarehouse;
    private Alert testAlert;
    private LocalDate today;
    private LocalDateTime testTime;

    @BeforeEach
    public void setup() {
        today = LocalDate.now();
        testTime = LocalDateTime.now();

        // Setup test medicine
        testMedicine = new Medicine();
        testMedicine.setMedicineId(1L);
        testMedicine.setName("Test Medicine");
        testMedicine.setManufacturer("Test Manufacturer");

        // Setup test warehouse
        testWarehouse = new Warehouse();
        testWarehouse.setWarehouseId(1L);
        testWarehouse.setName("Main Warehouse");
        testWarehouse.setLocation("Test Location");

        // Setup test batch
        testBatch = new Batch();
        testBatch.setBatchId(1L);
        testBatch.setMedicine(testMedicine);
        testBatch.setWarehouse(testWarehouse);
        testBatch.setLotNumber("LOT001");
        testBatch.setQuantity(100);
        testBatch.setExpiryDate(today.plusDays(30));
        testBatch.setStatus("AVAILABLE");

        // Setup test alert
        testAlert = new Alert();
        testAlert.setAlertId(1L);
        testAlert.setAlertType("EXPIRING_SOON");
        testAlert.setSeverity("HIGH");
        testAlert.setStatus("OPEN");
        testAlert.setMessage("Test Alert");
        testAlert.setDescription("Test Description");
        testAlert.setCreatedAt(testTime);
        testAlert.setBatch(testBatch);
        testAlert.setMedicine(testMedicine);
        testAlert.setWarehouse(testWarehouse);
    }

    // ==========================================
    // 1. BLACK-BOX TESTING (EP, BVA)
    // ==========================================

    /**
     * Kiểm thử Hộp đen: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
     * Target Hàm: scanAllAlerts()
     * Phân tích kết quả trả về:
     * - Empty batch list -> trả về 0
     * - Single batch list -> trả về số lượng alerts được tạo
     * - Multiple batches list -> trả về tổng số alerts
     */
    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra scanAllAlerts() với các trường hợp khác nhau")
    public void testScanAllAlerts_EquivalencePartition_BoundaryValue() {
        // Case 1: Empty batch list - Phân vùng rỗng
        when(batchRepository.findAll()).thenReturn(new ArrayList<>());
        when(alertRepository.findActiveAlerts()).thenReturn(new ArrayList<>());

        int result1 = alertScanService.scanAllAlerts();
        assertEquals(0, result1);

        // Case 2: Single batch - Giá trị biên dưới
        when(batchRepository.findAll()).thenReturn(List.of(testBatch));
        when(alertRepository.findActiveAlertByBatchAndType(anyLong(), anyString()))
            .thenReturn(new ArrayList<>());
        when(alertRepository.findActiveAlerts()).thenReturn(new ArrayList<>());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert saved = invocation.getArgument(0);
            if (saved.getAlertId() == null) {
                saved.setAlertId(1L);
            }
            return saved;
        });

        int result2 = alertScanService.scanAllAlerts();
        assertTrue(result2 >= 0);

        // Case 3: Multiple batches - Phân vùng bình thường
        Batch batch2 = new Batch();
        batch2.setBatchId(2L);
        batch2.setQuantity(5); // Low stock
        batch2.setExpiryDate(today.plusDays(5)); // Expiring soon
        batch2.setMedicine(testMedicine);
        batch2.setWarehouse(testWarehouse);
        batch2.setLotNumber("LOT002");

        when(batchRepository.findAll()).thenReturn(List.of(testBatch, batch2));

        int result3 = alertScanService.scanAllAlerts();
        assertTrue(result3 >= 0);

        verify(batchRepository, atLeastOnce()).findAll();
        verify(alertRepository, atLeastOnce()).findActiveAlerts();
    }

    // ==========================================
    // 2. WHITE-BOX TESTING (CFG, DFG)
    // ==========================================

    /**
     * Kiểm thử luồng điều khiển (CFG - Branch Coverage)
     * Dành cho hàm scanExpiredBatch(Batch batch):
     * - Branch 1: batch.getExpiryDate().isBefore(today) -> true -> check existing alerts
     * - Branch 2: existingAlerts.isEmpty() -> true -> create new alert
     * - Branch 3: existingAlerts.isEmpty() -> false -> no action
     * - Branch 4: expiry date not before today -> return false
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh scanExpiredBatch() - Success path")
    public void testScanExpiredBatch_Success_BranchCoverage() {
        // Set batch to expired
        testBatch.setExpiryDate(today.minusDays(1));

        when(alertRepository.findActiveAlertByBatchAndType(1L, "EXPIRED"))
            .thenReturn(new ArrayList<>());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert saved = invocation.getArgument(0);
            saved.setAlertId(1L);
            return saved;
        });

        // Use reflection to test private method
        boolean result = invokePrivateScanExpiredBatch(testBatch);

        assertTrue(result);
        verify(alertRepository, times(1)).findActiveAlertByBatchAndType(1L, "EXPIRED");
        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh scanExpiredBatch() - Existing alert")
    public void testScanExpiredBatch_ExistingAlert_BranchCoverage() {
        // Set batch to expired
        testBatch.setExpiryDate(today.minusDays(1));

        Alert existingAlert = new Alert();
        existingAlert.setAlertId(1L);
        existingAlert.setAlertType("EXPIRED");
        existingAlert.setStatus("OPEN");

        when(alertRepository.findActiveAlertByBatchAndType(1L, "EXPIRED"))
            .thenReturn(List.of(existingAlert));

        boolean result = invokePrivateScanExpiredBatch(testBatch);

        assertFalse(result);
        verify(alertRepository, times(1)).findActiveAlertByBatchAndType(1L, "EXPIRED");
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh scanExpiredBatch() - Not expired")
    public void testScanExpiredBatch_NotExpired_BranchCoverage() {
        // Set batch to future date
        testBatch.setExpiryDate(today.plusDays(30));

        boolean result = invokePrivateScanExpiredBatch(testBatch);

        assertFalse(result);
        verify(alertRepository, never()).findActiveAlertByBatchAndType(anyLong(), anyString());
        verify(alertRepository, never()).save(any(Alert.class));
    }

    /**
     * Phân tích luồng dữ liệu (DFG - Data Flow Graph)
     * Hàm: scanExpiringBatch(Batch batch)
     * Quy trình DFG:
     * 1. Use batch.getExpiryDate() -> Compare with today
     * 2. Use ChronoUnit.DAYS.between() -> Calculate daysUntilExpiry
     * 3. Use daysUntilExpiry -> Determine severity based on thresholds
     * 4. Use severity -> Check existing alerts
     * 5. Use existingAlerts -> Create new or update existing
     */
    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng dữ liệu scanExpiringBatch() - Critical severity")
    public void testScanExpiringBatch_CriticalSeverity_DataFlow() {
        // Set batch to expire in 5 days (critical)
        testBatch.setExpiryDate(today.plusDays(5));

        when(alertRepository.findActiveAlertByBatchAndType(1L, "EXPIRING_SOON"))
            .thenReturn(new ArrayList<>());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert saved = invocation.getArgument(0);
            saved.setAlertId(1L);
            return saved;
        });

        boolean result = invokePrivateScanExpiringBatch(testBatch);

        assertTrue(result);
        verify(alertRepository, times(1)).findActiveAlertByBatchAndType(1L, "EXPIRING_SOON");
        verify(alertRepository, times(1)).save(any(Alert.class));

        // Verify the alert was created with CRITICAL severity
        verify(alertRepository).save(argThat(alert -> 
            "CRITICAL".equals(alert.getSeverity()) && 
            "EXPIRING_SOON".equals(alert.getAlertType())
        ));
    }

    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng dữ liệu scanExpiringBatch() - High severity")
    public void testScanExpiringBatch_HighSeverity_DataFlow() {
        // Set batch to expire in 15 days (high)
        testBatch.setExpiryDate(today.plusDays(15));

        when(alertRepository.findActiveAlertByBatchAndType(1L, "EXPIRING_SOON"))
            .thenReturn(new ArrayList<>());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert saved = invocation.getArgument(0);
            saved.setAlertId(1L);
            return saved;
        });

        boolean result = invokePrivateScanExpiringBatch(testBatch);

        assertTrue(result);
        verify(alertRepository, times(1)).findActiveAlertByBatchAndType(1L, "EXPIRING_SOON");
        verify(alertRepository, times(1)).save(any(Alert.class));

        // Verify the alert was created with HIGH severity
        verify(alertRepository).save(argThat(alert -> 
            "HIGH".equals(alert.getSeverity()) && 
            "EXPIRING_SOON".equals(alert.getAlertType())
        ));
    }

    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng dữ liệu scanExpiringBatch() - Medium severity")
    public void testScanExpiringBatch_MediumSeverity_DataFlow() {
        // Set batch to expire in 45 days (medium)
        testBatch.setExpiryDate(today.plusDays(45));

        when(alertRepository.findActiveAlertByBatchAndType(1L, "EXPIRING_SOON"))
            .thenReturn(new ArrayList<>());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert saved = invocation.getArgument(0);
            saved.setAlertId(1L);
            return saved;
        });

        boolean result = invokePrivateScanExpiringBatch(testBatch);

        assertTrue(result);
        verify(alertRepository, times(1)).findActiveAlertByBatchAndType(1L, "EXPIRING_SOON");
        verify(alertRepository, times(1)).save(any(Alert.class));

        // Verify the alert was created with MEDIUM severity
        verify(alertRepository).save(argThat(alert -> 
            "MEDIUM".equals(alert.getSeverity()) && 
            "EXPIRING_SOON".equals(alert.getAlertType())
        ));
    }

    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng dữ liệu scanExpiringBatch() - Update existing alert")
    public void testScanExpiringBatch_UpdateExistingAlert_DataFlow() {
        // Set batch to expire in 5 days (critical)
        testBatch.setExpiryDate(today.plusDays(5));

        Alert existingAlert = new Alert();
        existingAlert.setAlertId(1L);
        existingAlert.setAlertType("EXPIRING_SOON");
        existingAlert.setSeverity("HIGH"); // Different severity
        existingAlert.setStatus("OPEN");

        when(alertRepository.findActiveAlertByBatchAndType(1L, "EXPIRING_SOON"))
            .thenReturn(List.of(existingAlert));
        when(alertRepository.save(any(Alert.class))).thenReturn(existingAlert);

        boolean result = invokePrivateScanExpiringBatch(testBatch);

        assertFalse(result); // Returns false because it's an update, not new creation
        verify(alertRepository, times(1)).findActiveAlertByBatchAndType(1L, "EXPIRING_SOON");
        verify(alertRepository, times(1)).save(existingAlert);

        // Verify the alert severity was updated
        assertEquals("CRITICAL", existingAlert.getSeverity());
    }

    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng dữ liệu scanExpiringBatch() - No alert needed")
    public void testScanExpiringBatch_NoAlertNeeded_DataFlow() {
        // Set batch to expire in 90 days (no alert needed)
        testBatch.setExpiryDate(today.plusDays(90));

        boolean result = invokePrivateScanExpiringBatch(testBatch);

        assertFalse(result);
        verify(alertRepository, never()).findActiveAlertByBatchAndType(anyLong(), anyString());
        verify(alertRepository, never()).save(any(Alert.class));
    }

    /**
     * Kiểm thử luồng điều khiển cho scanLowStockBatch()
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh scanLowStockBatch() - Critical severity")
    public void testScanLowStockBatch_CriticalSeverity_BranchCoverage() {
        // Set batch to critical low stock
        testBatch.setQuantity(5);

        when(alertRepository.findActiveAlertByBatchAndType(1L, "LOW_STOCK"))
            .thenReturn(new ArrayList<>());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert saved = invocation.getArgument(0);
            saved.setAlertId(1L);
            return saved;
        });

        boolean result = invokePrivateScanLowStockBatch(testBatch);

        assertTrue(result);
        verify(alertRepository, times(1)).findActiveAlertByBatchAndType(1L, "LOW_STOCK");
        verify(alertRepository, times(1)).save(any(Alert.class));

        // Verify the alert was created with CRITICAL severity
        verify(alertRepository).save(argThat(alert -> 
            "CRITICAL".equals(alert.getSeverity()) && 
            "LOW_STOCK".equals(alert.getAlertType())
        ));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh scanLowStockBatch() - High severity")
    public void testScanLowStockBatch_HighSeverity_BranchCoverage() {
        // Set batch to high low stock
        testBatch.setQuantity(15);

        when(alertRepository.findActiveAlertByBatchAndType(1L, "LOW_STOCK"))
            .thenReturn(new ArrayList<>());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert saved = invocation.getArgument(0);
            saved.setAlertId(1L);
            return saved;
        });

        boolean result = invokePrivateScanLowStockBatch(testBatch);

        assertTrue(result);
        verify(alertRepository, times(1)).findActiveAlertByBatchAndType(1L, "LOW_STOCK");
        verify(alertRepository, times(1)).save(any(Alert.class));

        // Verify the alert was created with HIGH severity
        verify(alertRepository).save(argThat(alert -> 
            "HIGH".equals(alert.getSeverity()) && 
            "LOW_STOCK".equals(alert.getAlertType())
        ));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh scanLowStockBatch() - Medium severity")
    public void testScanLowStockBatch_MediumSeverity_BranchCoverage() {
        // Set batch to medium low stock
        testBatch.setQuantity(30);

        when(alertRepository.findActiveAlertByBatchAndType(1L, "LOW_STOCK"))
            .thenReturn(new ArrayList<>());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert saved = invocation.getArgument(0);
            saved.setAlertId(1L);
            return saved;
        });

        boolean result = invokePrivateScanLowStockBatch(testBatch);

        assertTrue(result);
        verify(alertRepository, times(1)).findActiveAlertByBatchAndType(1L, "LOW_STOCK");
        verify(alertRepository, times(1)).save(any(Alert.class));

        // Verify the alert was created with MEDIUM severity
        verify(alertRepository).save(argThat(alert -> 
            "MEDIUM".equals(alert.getSeverity()) && 
            "LOW_STOCK".equals(alert.getAlertType())
        ));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh scanLowStockBatch() - No alert needed")
    public void testScanLowStockBatch_NoAlertNeeded_BranchCoverage() {
        // Set batch to sufficient stock
        testBatch.setQuantity(100);

        boolean result = invokePrivateScanLowStockBatch(testBatch);

        assertFalse(result);
        verify(alertRepository, never()).findActiveAlertByBatchAndType(anyLong(), anyString());
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh scanLowStockBatch() - Update existing alert")
    public void testScanLowStockBatch_UpdateExistingAlert_BranchCoverage() {
        // Set batch to critical low stock
        testBatch.setQuantity(5);

        Alert existingAlert = new Alert();
        existingAlert.setAlertId(1L);
        existingAlert.setAlertType("LOW_STOCK");
        existingAlert.setSeverity("HIGH"); // Different severity
        existingAlert.setStatus("OPEN");

        when(alertRepository.findActiveAlertByBatchAndType(1L, "LOW_STOCK"))
            .thenReturn(List.of(existingAlert));
        when(alertRepository.save(any(Alert.class))).thenReturn(existingAlert);

        boolean result = invokePrivateScanLowStockBatch(testBatch);

        assertFalse(result); // Returns false because it's an update, not new creation
        verify(alertRepository, times(1)).findActiveAlertByBatchAndType(1L, "LOW_STOCK");
        verify(alertRepository, times(1)).save(existingAlert);

        // Verify the alert severity was updated
        assertEquals("CRITICAL", existingAlert.getSeverity());
    }

    // ==========================================
    // 3. PRIVATE METHOD TESTING
    // ==========================================

    @Test
    @DisplayName("Private Method | createAlert: Test complete alert creation")
    public void testCreateAlert_CompleteCreation() throws Exception {
        // Use reflection to test private method
        invokePrivateCreateAlert("TEST_TYPE", "HIGH", "Test Message", "Test Description", testBatch);

        verify(alertRepository, times(1)).save(argThat(alert -> 
            "TEST_TYPE".equals(alert.getAlertType()) &&
            "HIGH".equals(alert.getSeverity()) &&
            "OPEN".equals(alert.getStatus()) &&
            "Test Message".equals(alert.getMessage()) &&
            "Test Description".equals(alert.getDescription()) &&
            alert.getCreatedAt() != null &&
            testBatch.equals(alert.getBatch()) &&
            testMedicine.equals(alert.getMedicine()) &&
            testWarehouse.equals(alert.getWarehouse())
        ));
    }

    @Test
    @DisplayName("Private Method | createAlert: Test with null medicine and warehouse")
    public void testCreateAlert_NullMedicineAndWarehouse() throws Exception {
        testBatch.setMedicine(null);
        testBatch.setWarehouse(null);

        invokePrivateCreateAlert("TEST_TYPE", "HIGH", "Test Message", "Test Description", testBatch);

        verify(alertRepository, times(1)).save(argThat(alert -> 
            alert.getMedicine() == null &&
            alert.getWarehouse() == null
        ));
    }

    // ==========================================
    // 4. AUTO-RESOLVE TESTING
    // ==========================================

    @Test
    @DisplayName("Auto-Resolve | Test LOW_STOCK alert resolution")
    public void testAutoResolveAlerts_LowStockResolution() {
        Alert lowStockAlert = new Alert();
        lowStockAlert.setAlertId(1L);
        lowStockAlert.setAlertType("LOW_STOCK");
        lowStockAlert.setStatus("OPEN");
        lowStockAlert.setBatch(testBatch);

        // Set batch quantity above threshold
        testBatch.setQuantity(100);

        when(alertRepository.findActiveAlerts()).thenReturn(List.of(lowStockAlert));
        when(alertRepository.save(any(Alert.class))).thenReturn(lowStockAlert);

        invokePrivateAutoResolveAlerts();

        verify(alertRepository, times(1)).findActiveAlerts();
        verify(alertRepository, times(1)).save(lowStockAlert);
        assertEquals("RESOLVED", lowStockAlert.getStatus());
        assertNotNull(lowStockAlert.getResolvedAt());
    }

    @Test
    @DisplayName("Auto-Resolve | Test EXPIRING_SOON alert resolution")
    public void testAutoResolveAlerts_ExpiringSoonResolution() {
        Alert expiringAlert = new Alert();
        expiringAlert.setAlertId(1L);
        expiringAlert.setAlertType("EXPIRING_SOON");
        expiringAlert.setStatus("OPEN");
        expiringAlert.setBatch(testBatch);

        // Set batch to expired
        testBatch.setExpiryDate(today.minusDays(1));

        when(alertRepository.findActiveAlerts()).thenReturn(List.of(expiringAlert));
        when(alertRepository.save(any(Alert.class))).thenReturn(expiringAlert);

        invokePrivateAutoResolveAlerts();

        verify(alertRepository, times(1)).findActiveAlerts();
        verify(alertRepository, times(1)).save(expiringAlert);
        assertEquals("RESOLVED", expiringAlert.getStatus());
        assertNotNull(expiringAlert.getResolvedAt());
    }

    @Test
    @DisplayName("Auto-Resolve | Test no resolution needed")
    public void testAutoResolveAlerts_NoResolutionNeeded() {
        Alert lowStockAlert = new Alert();
        lowStockAlert.setAlertId(1L);
        lowStockAlert.setAlertType("LOW_STOCK");
        lowStockAlert.setStatus("OPEN");
        lowStockAlert.setBatch(testBatch);

        // Set batch quantity below threshold (should not resolve)
        testBatch.setQuantity(5);

        when(alertRepository.findActiveAlerts()).thenReturn(List.of(lowStockAlert));

        invokePrivateAutoResolveAlerts();

        verify(alertRepository, times(1)).findActiveAlerts();
        verify(alertRepository, never()).save(any(Alert.class));
        assertEquals("OPEN", lowStockAlert.getStatus());
        assertNull(lowStockAlert.getResolvedAt());
    }

    @Test
    @DisplayName("Auto-Resolve | Test alert with null batch")
    public void testAutoResolveAlerts_NullBatch() {
        Alert alertWithNullBatch = new Alert();
        alertWithNullBatch.setAlertId(1L);
        alertWithNullBatch.setAlertType("LOW_STOCK");
        alertWithNullBatch.setStatus("OPEN");
        alertWithNullBatch.setBatch(null);

        when(alertRepository.findActiveAlerts()).thenReturn(List.of(alertWithNullBatch));

        invokePrivateAutoResolveAlerts();

        verify(alertRepository, times(1)).findActiveAlerts();
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    @DisplayName("Auto-Resolve | Test EXPIRED alert type (no auto-resolution)")
    public void testAutoResolveAlerts_ExpiredAlertType() {
        Alert expiredAlert = new Alert();
        expiredAlert.setAlertId(1L);
        expiredAlert.setAlertType("EXPIRED");
        expiredAlert.setStatus("OPEN");
        expiredAlert.setBatch(testBatch);

        when(alertRepository.findActiveAlerts()).thenReturn(List.of(expiredAlert));

        invokePrivateAutoResolveAlerts();

        verify(alertRepository, times(1)).findActiveAlerts();
        verify(alertRepository, never()).save(any(Alert.class));
        assertEquals("OPEN", expiredAlert.getStatus());
    }

    // ==========================================
    // 5. SUPPLEMENTARY COVERAGE TESTS
    // ==========================================

    @Test
    @DisplayName("Supplementary: Test scanAllAlerts with null expiry date")
    public void testScanAllAlerts_NullExpiryDate() {
        testBatch.setExpiryDate(null);

        when(batchRepository.findAll()).thenReturn(List.of(testBatch));
        when(alertRepository.findActiveAlerts()).thenReturn(new ArrayList<>());

        int result = alertScanService.scanAllAlerts();

        assertEquals(0, result);
        verify(alertRepository, never()).findActiveAlertByBatchAndType(anyLong(), anyString());
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    @DisplayName("Supplementary: Test scanAllAlerts with null quantity")
    public void testScanAllAlerts_NullQuantity() {
        testBatch.setQuantity(null);

        when(batchRepository.findAll()).thenReturn(List.of(testBatch));
        when(alertRepository.findActiveAlerts()).thenReturn(new ArrayList<>());

        int result = alertScanService.scanAllAlerts();

        assertEquals(0, result);
        verify(alertRepository, never()).findActiveAlertByBatchAndType(anyLong(), anyString());
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    @DisplayName("Supplementary: Test scanAllAlerts with null medicine")
    public void testScanAllAlerts_NullMedicine() {
        testBatch.setMedicine(null);
        testBatch.setExpiryDate(today.minusDays(1)); // Expired

        when(batchRepository.findAll()).thenReturn(List.of(testBatch));
        when(alertRepository.findActiveAlertByBatchAndType(anyLong(), anyString()))
            .thenReturn(new ArrayList<>());
        when(alertRepository.findActiveAlerts()).thenReturn(new ArrayList<>());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert saved = invocation.getArgument(0);
            saved.setAlertId(1L);
            return saved;
        });

        int result = alertScanService.scanAllAlerts();

        assertTrue(result > 0);
        verify(alertRepository, atLeastOnce()).save(argThat(alert -> 
            alert.getMedicine() == null
        ));
    }

    @Test
    @DisplayName("Supplementary: Test scanAllAlerts with null warehouse")
    public void testScanAllAlerts_NullWarehouse() {
        testBatch.setWarehouse(null);
        testBatch.setQuantity(5); // Low stock

        when(batchRepository.findAll()).thenReturn(List.of(testBatch));
        when(alertRepository.findActiveAlertByBatchAndType(anyLong(), anyString()))
            .thenReturn(new ArrayList<>());
        when(alertRepository.findActiveAlerts()).thenReturn(new ArrayList<>());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert saved = invocation.getArgument(0);
            saved.setAlertId(1L);
            return saved;
        });

        int result = alertScanService.scanAllAlerts();

        assertTrue(result > 0);
        verify(alertRepository, atLeastOnce()).save(argThat(alert -> 
            alert.getWarehouse() == null
        ));
    }

    @Test
    @DisplayName("Supplementary: Test repository exceptions")
    public void testRepositoryExceptions() {
        // Test batch repository exception
        when(batchRepository.findAll()).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            alertScanService.scanAllAlerts();
        });

        // Test alert repository exception in auto-resolve
        when(alertRepository.findActiveAlerts()).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            invokePrivateAutoResolveAlerts();
        });
    }

    @Test
    @DisplayName("Supplementary: Test scanExpiringBatch with exactly threshold boundaries")
    public void testScanExpiringBatch_ThresholdBoundaries() {
        // Test exactly 6 days (critical threshold - should be CRITICAL since < 7)
        testBatch.setExpiryDate(today.plusDays(6));

        when(alertRepository.findActiveAlertByBatchAndType(1L, "EXPIRING_SOON"))
            .thenReturn(new ArrayList<>());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert saved = invocation.getArgument(0);
            saved.setAlertId(1L);
            return saved;
        });

        boolean result = invokePrivateScanExpiringBatch(testBatch);

        assertTrue(result);
        verify(alertRepository).save(argThat(alert -> 
            "CRITICAL".equals(alert.getSeverity())
        ));
    }

    @Test
    @DisplayName("Supplementary: Test scanLowStockBatch with exactly threshold boundaries")
    public void testScanLowStockBatch_ThresholdBoundaries() {
        // Test exactly 9 units (critical threshold - should be CRITICAL since < 10)
        testBatch.setQuantity(9);

        when(alertRepository.findActiveAlertByBatchAndType(1L, "LOW_STOCK"))
            .thenReturn(new ArrayList<>());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert saved = invocation.getArgument(0);
            saved.setAlertId(1L);
            return saved;
        });

        boolean result = invokePrivateScanLowStockBatch(testBatch);

        assertTrue(result);
        verify(alertRepository).save(argThat(alert -> 
            "CRITICAL".equals(alert.getSeverity())
        ));
    }

    @Test
    @DisplayName("Supplementary: Test autoResolveAlerts with null batch quantity")
    public void testAutoResolveAlerts_NullBatchQuantity() {
        Alert lowStockAlert = new Alert();
        lowStockAlert.setAlertId(1L);
        lowStockAlert.setAlertType("LOW_STOCK");
        lowStockAlert.setStatus("OPEN");
        lowStockAlert.setBatch(testBatch);

        // Set batch quantity to null
        testBatch.setQuantity(null);

        when(alertRepository.findActiveAlerts()).thenReturn(List.of(lowStockAlert));

        invokePrivateAutoResolveAlerts();

        verify(alertRepository, times(1)).findActiveAlerts();
        verify(alertRepository, never()).save(any(Alert.class));
        assertEquals("OPEN", lowStockAlert.getStatus());
    }

    @Test
    @DisplayName("Supplementary: Test autoResolveAlerts with null expiry date")
    public void testAutoResolveAlerts_NullExpiryDate() {
        Alert expiringAlert = new Alert();
        expiringAlert.setAlertId(1L);
        expiringAlert.setAlertType("EXPIRING_SOON");
        expiringAlert.setStatus("OPEN");
        expiringAlert.setBatch(testBatch);

        // Set batch expiry date to null
        testBatch.setExpiryDate(null);

        when(alertRepository.findActiveAlerts()).thenReturn(List.of(expiringAlert));

        invokePrivateAutoResolveAlerts();

        verify(alertRepository, times(1)).findActiveAlerts();
        verify(alertRepository, never()).save(any(Alert.class));
        assertEquals("OPEN", expiringAlert.getStatus());
    }

    @Test
    @DisplayName("Supplementary: Test autoResolveAlerts with unknown alert type")
    public void testAutoResolveAlerts_UnknownAlertType() {
        Alert unknownAlert = new Alert();
        unknownAlert.setAlertId(1L);
        unknownAlert.setAlertType("UNKNOWN_TYPE");
        unknownAlert.setStatus("OPEN");
        unknownAlert.setBatch(testBatch);

        when(alertRepository.findActiveAlerts()).thenReturn(List.of(unknownAlert));

        invokePrivateAutoResolveAlerts();

        verify(alertRepository, times(1)).findActiveAlerts();
        verify(alertRepository, never()).save(any(Alert.class));
        assertEquals("OPEN", unknownAlert.getStatus());
    }

    // ==========================================
    // 6. SCHEDULED METHOD TESTING
    // ==========================================

    @Test
    @DisplayName("Scheduled Method | Test scheduledAlertScan")
    public void testScheduledAlertScan() {
        when(batchRepository.findAll()).thenReturn(new ArrayList<>());
        when(alertRepository.findActiveAlerts()).thenReturn(new ArrayList<>());

        alertScanService.scheduledAlertScan();

        verify(batchRepository, times(1)).findAll();
        verify(alertRepository, times(1)).findActiveAlerts();
    }

    // ==========================================
    // 7. HELPER METHODS FOR TESTING PRIVATE METHODS
    // ==========================================

    private boolean invokePrivateScanExpiredBatch(Batch batch) {
        try {
            java.lang.reflect.Method method = AlertScanService.class.getDeclaredMethod("scanExpiredBatch", Batch.class);
            method.setAccessible(true);
            return (boolean) method.invoke(alertScanService, batch);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke private method", e);
        }
    }

    private boolean invokePrivateScanExpiringBatch(Batch batch) {
        try {
            java.lang.reflect.Method method = AlertScanService.class.getDeclaredMethod("scanExpiringBatch", Batch.class);
            method.setAccessible(true);
            return (boolean) method.invoke(alertScanService, batch);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke private method", e);
        }
    }

    private boolean invokePrivateScanLowStockBatch(Batch batch) {
        try {
            java.lang.reflect.Method method = AlertScanService.class.getDeclaredMethod("scanLowStockBatch", Batch.class);
            method.setAccessible(true);
            return (boolean) method.invoke(alertScanService, batch);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke private method", e);
        }
    }

    private void invokePrivateCreateAlert(String alertType, String severity, String message, 
                                        String description, Batch batch) {
        try {
            java.lang.reflect.Method method = AlertScanService.class.getDeclaredMethod(
                "createAlert", String.class, String.class, String.class, String.class, Batch.class);
            method.setAccessible(true);
            method.invoke(alertScanService, alertType, severity, message, description, batch);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke private method", e);
        }
    }

    private void invokePrivateAutoResolveAlerts() {
        try {
            java.lang.reflect.Method method = AlertScanService.class.getDeclaredMethod("autoResolveAlerts");
            method.setAccessible(true);
            method.invoke(alertScanService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke private method", e);
        }
    }
}
