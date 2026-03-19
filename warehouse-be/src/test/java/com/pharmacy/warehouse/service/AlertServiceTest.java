package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.dto.AlertHistoryResponse;
import com.pharmacy.warehouse.dto.AlertResponse;
import com.pharmacy.warehouse.dto.AlertStatsResponse;
import com.pharmacy.warehouse.dto.InventoryResponse;
import com.pharmacy.warehouse.model.*;
import com.pharmacy.warehouse.repository.*;
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
public class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertHistoryRepository alertHistoryRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private AlertService alertService;

    private Alert testAlert;
    private AlertHistory testAlertHistory;
    private Batch testBatch;
    private Medicine testMedicine;
    private Warehouse testWarehouse;
    private User testUser;
    private LocalDateTime testTime;

    @BeforeEach
    public void setup() {
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
        testBatch.setQuantity(5);
        testBatch.setExpiryDate(LocalDate.now().plusDays(30));
        testBatch.setStatus("AVAILABLE");

        // Setup test user
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("testuser");
        testUser.setFullName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setStatus("ACTIVE");

        // Setup test alert
        testAlert = new Alert();
        testAlert.setAlertId(1L);
        testAlert.setAlertType("LOW_STOCK");
        testAlert.setSeverity("HIGH");
        testAlert.setStatus("OPEN");
        testAlert.setMessage("Low stock alert");
        testAlert.setDescription("Stock level is low");
        testAlert.setCreatedAt(testTime);
        testAlert.setBatch(testBatch);
        testAlert.setMedicine(testMedicine);
        testAlert.setWarehouse(testWarehouse);

        // Setup test alert history
        testAlertHistory = new AlertHistory();
        testAlertHistory.setHistoryId(1L);
        testAlertHistory.setAlert(testAlert);
        testAlertHistory.setAction("CREATED");
        testAlertHistory.setOldStatus(null);
        testAlertHistory.setNewStatus("OPEN");
        testAlertHistory.setComment("Auto-generated alert");
        testAlertHistory.setTimestamp(testTime);
        testAlertHistory.setUser(testUser);
    }

    // ==========================================
    // 1. BLACK-BOX TESTING (EP, BVA)
    // ==========================================

    /**
     * Kiểm thử Hộp đen: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
     * Target Hàm: getAllAlerts(), getActiveAlerts(), getAlertsByType(), getAlertsBySeverity(), getAlertsByStatus()
     * Phân tích kết quả trả về:
     * - Empty list - Phân vùng rỗng
     * - Single item - Giá trị biên dưới
     * - Multiple items - Phân vùng bình thường
     */
    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra các query methods với các trường hợp khác nhau")
    public void testQueryMethods_EquivalencePartition_BoundaryValue() {
        // Case 1: Empty list - Phân vùng rỗng
        when(alertRepository.findAll()).thenReturn(new ArrayList<>());
        when(alertRepository.findActiveAlerts()).thenReturn(new ArrayList<>());
        when(alertRepository.findByAlertType("LOW_STOCK")).thenReturn(new ArrayList<>());
        when(alertRepository.findBySeverity("HIGH")).thenReturn(new ArrayList<>());
        when(alertRepository.findByStatus("OPEN")).thenReturn(new ArrayList<>());

        List<AlertResponse> result1 = alertService.getAllAlerts();
        List<AlertResponse> result2 = alertService.getActiveAlerts();
        List<AlertResponse> result3 = alertService.getAlertsByType("LOW_STOCK");
        List<AlertResponse> result4 = alertService.getAlertsBySeverity("HIGH");
        List<AlertResponse> result5 = alertService.getAlertsByStatus("OPEN");

        assertTrue(result1.isEmpty());
        assertTrue(result2.isEmpty());
        assertTrue(result3.isEmpty());
        assertTrue(result4.isEmpty());
        assertTrue(result5.isEmpty());

        // Case 2: Single item - Giá trị biên dưới
        when(alertRepository.findAll()).thenReturn(List.of(testAlert));
        when(alertRepository.findActiveAlerts()).thenReturn(List.of(testAlert));
        when(alertRepository.findByAlertType("LOW_STOCK")).thenReturn(List.of(testAlert));
        when(alertRepository.findBySeverity("HIGH")).thenReturn(List.of(testAlert));
        when(alertRepository.findByStatus("OPEN")).thenReturn(List.of(testAlert));

        List<AlertResponse> result6 = alertService.getAllAlerts();
        List<AlertResponse> result7 = alertService.getActiveAlerts();
        List<AlertResponse> result8 = alertService.getAlertsByType("LOW_STOCK");
        List<AlertResponse> result9 = alertService.getAlertsBySeverity("HIGH");
        List<AlertResponse> result10 = alertService.getAlertsByStatus("OPEN");

        assertEquals(1, result6.size());
        assertEquals(1, result7.size());
        assertEquals(1, result8.size());
        assertEquals(1, result9.size());
        assertEquals(1, result10.size());

        // Case 3: Multiple items - Phân vùng bình thường
        Alert alert2 = new Alert();
        alert2.setAlertId(2L);
        alert2.setAlertType("EXPIRING_SOON");
        alert2.setBatch(testBatch);
        alert2.setMedicine(testMedicine);

        when(alertRepository.findAll()).thenReturn(List.of(testAlert, alert2));

        List<AlertResponse> result11 = alertService.getAllAlerts();
        assertEquals(2, result11.size());

        verify(alertRepository, atLeastOnce()).findAll();
        verify(alertRepository, atLeastOnce()).findActiveAlerts();
        verify(alertRepository, atLeastOnce()).findByAlertType(anyString());
        verify(alertRepository, atLeastOnce()).findBySeverity(anyString());
        verify(alertRepository, atLeastOnce()).findByStatus(anyString());
    }

    // ==========================================
    // 2. WHITE-BOX TESTING (CFG, DFG)
    // ==========================================

    /**
     * Kiểm thử luồng điều khiển (CFG - Branch Coverage)
     * Dành cho hàm getAlertById(Long id):
     * - Branch 1: alertRepository.findById() trả về Optional.isPresent() -> tiếp tục xử lý
     * - Branch 2: alertRepository.findById() trả về Optional.isEmpty() -> ném RuntimeException
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getAlertById() - Success path")
    public void testGetAlertById_Success_BranchCoverage() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));

        AlertResponse result = alertService.getAlertById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getAlertId());
        assertEquals("LOW_STOCK", result.getAlertType());
        assertEquals("HIGH", result.getSeverity());
        assertEquals("OPEN", result.getStatus());
        assertEquals("Low stock alert", result.getMessage());
        assertEquals(testTime, result.getCreatedAt());
        assertEquals(1L, result.getBatchId());
        assertEquals("LOT001", result.getLotNumber());
        assertEquals(1L, result.getMedicineId());
        assertEquals("Test Medicine", result.getMedicineName());
        assertEquals(1L, result.getWarehouseId());
        assertEquals("Main Warehouse", result.getWarehouseName());

        verify(alertRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getAlertById() - Exception path")
    public void testGetAlertById_NotFound_BranchCoverage() {
        when(alertRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            alertService.getAlertById(999L);
        });

        assertEquals("Alert not found with id: 999", exception.getMessage());
        verify(alertRepository, times(1)).findById(999L);
    }

    /**
     * Phân tích luồng dữ liệu (DFG - Data Flow Graph)
     * Hàm: getAlertStats()
     * Quy trình DFG:
     * 1. Use repository.countActiveAlertsByType() -> Define individual counts
     * 2. Use individual counts -> Calculate total
     * 3. Use counts -> Build AlertStatsResponse object
     * 4. Return AlertStatsResponse
     */
    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng dữ liệu getAlertStats() đầy đủ")
    public void testGetAlertStats_CompleteDataFlow() {
        when(alertRepository.countActiveAlertsByType("LOW_STOCK")).thenReturn(5L);
        when(alertRepository.countActiveAlertsByType("EXPIRING_SOON")).thenReturn(3L);
        when(alertRepository.countActiveAlertsByType("EXPIRED")).thenReturn(2L);
        when(alertRepository.countActiveAlertsByType("SYSTEM")).thenReturn(1L);

        AlertStatsResponse result = alertService.getAlertStats();

        assertNotNull(result);
        assertEquals(5L, result.getLowStockCount());
        assertEquals(3L, result.getExpiringSoonCount());
        assertEquals(2L, result.getExpiredCount());
        assertEquals(1L, result.getSystemWarningsCount());
        assertEquals(11L, result.getTotalActiveAlerts()); // 5+3+2+1

        verify(alertRepository, times(1)).countActiveAlertsByType("LOW_STOCK");
        verify(alertRepository, times(1)).countActiveAlertsByType("EXPIRING_SOON");
        verify(alertRepository, times(1)).countActiveAlertsByType("EXPIRED");
        verify(alertRepository, times(1)).countActiveAlertsByType("SYSTEM");
    }

    /**
     * Kiểm thử luồng điều khiển cho resolveAlert()
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh resolveAlert() - Success with user found")
    public void testResolveAlert_SuccessWithUser_BranchCoverage() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);
        when(alertHistoryRepository.save(any(AlertHistory.class))).thenReturn(testAlertHistory);

        AlertResponse result = alertService.resolveAlert(1L, "testuser", "Resolved manually");

        assertNotNull(result);
        assertEquals("RESOLVED", result.getStatus());
        assertNotNull(result.getResolvedAt());
        assertEquals(1L, result.getResolvedByUserId());
        assertEquals("testuser", result.getResolvedByUsername());

        verify(alertRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(alertRepository, times(1)).save(testAlert);
        verify(alertHistoryRepository, times(1)).save(any(AlertHistory.class));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh resolveAlert() - Success with user not found")
    public void testResolveAlert_SuccessWithoutUser_BranchCoverage() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        when(userRepository.findByUsername("unknownuser")).thenReturn(Optional.empty());
        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);
        when(alertHistoryRepository.save(any(AlertHistory.class))).thenReturn(testAlertHistory);

        AlertResponse result = alertService.resolveAlert(1L, "unknownuser", "Resolved manually");

        assertNotNull(result);
        assertEquals("RESOLVED", result.getStatus());
        assertNotNull(result.getResolvedAt());
        assertNull(result.getResolvedByUserId());
        assertNull(result.getResolvedByUsername());

        verify(alertRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findByUsername("unknownuser");
        verify(alertRepository, times(1)).save(testAlert);
        verify(alertHistoryRepository, times(1)).save(any(AlertHistory.class));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh resolveAlert() - Alert not found")
    public void testResolveAlert_NotFound_BranchCoverage() {
        when(alertRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            alertService.resolveAlert(999L, "testuser", "Resolved manually");
        });

        assertEquals("Alert not found with id: 999", exception.getMessage());
        verify(alertRepository, times(1)).findById(999L);
        verify(userRepository, never()).findByUsername(anyString());
        verify(alertRepository, never()).save(any());
        verify(alertHistoryRepository, never()).save(any());
    }

    /**
     * Kiểm thử luồng điều khiển cho updateAlertStatus()
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh updateAlertStatus() - RESOLVED status")
    public void testUpdateAlertStatus_ResolvedStatus_BranchCoverage() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);
        when(alertHistoryRepository.save(any(AlertHistory.class))).thenReturn(testAlertHistory);

        AlertResponse result = alertService.updateAlertStatus(1L, "RESOLVED", "testuser");

        assertNotNull(result);
        assertEquals("RESOLVED", result.getStatus());
        assertNotNull(result.getResolvedAt());
        assertEquals(1L, result.getResolvedByUserId());

        verify(alertRepository, times(1)).save(testAlert);
        verify(alertHistoryRepository, times(1)).save(any(AlertHistory.class));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh updateAlertStatus() - Non-RESOLVED status")
    public void testUpdateAlertStatus_NonResolvedStatus_BranchCoverage() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);
        when(alertHistoryRepository.save(any(AlertHistory.class))).thenReturn(testAlertHistory);

        AlertResponse result = alertService.updateAlertStatus(1L, "IN_PROGRESS", "testuser");

        assertNotNull(result);
        assertEquals("IN_PROGRESS", result.getStatus());
        assertNull(result.getResolvedAt()); // Should not be set for non-RESOLVED status
        assertNull(result.getResolvedByUserId());

        verify(alertRepository, times(1)).save(testAlert);
        verify(alertHistoryRepository, times(1)).save(any(AlertHistory.class));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh updateAlertStatus() - Alert not found")
    public void testUpdateAlertStatus_NotFound_BranchCoverage() {
        when(alertRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            alertService.updateAlertStatus(999L, "RESOLVED", "testuser");
        });

        assertEquals("Alert not found with id: 999", exception.getMessage());
        verify(alertRepository, times(1)).findById(999L);
        verify(userRepository, never()).findByUsername(anyString());
        verify(alertRepository, never()).save(any());
        verify(alertHistoryRepository, never()).save(any());
    }

    // ==========================================
    // 3. ALERT GENERATION TESTING
    // ==========================================

    @Test
    @DisplayName("Alert Generation | Test checkAndGenerateAlerts() - Complete workflow")
    public void testCheckAndGenerateAlerts_CompleteWorkflow() {
        when(inventoryService.getLowStockInventory()).thenReturn(new ArrayList<>());
        when(alertRepository.findByAlertType("LOW_STOCK")).thenReturn(new ArrayList<>());
        when(batchRepository.findAll()).thenReturn(new ArrayList<>());

        alertService.checkAndGenerateAlerts();

        verify(inventoryService, times(1)).getLowStockInventory();
        verify(batchRepository, times(2)).findAll();
    }

    @Test
    @DisplayName("Alert Generation | Test checkLowStockAlerts() - Create new alert")
    public void testCheckLowStockAlerts_CreateNewAlert() {
        InventoryResponse lowStock = new InventoryResponse();
        lowStock.setMedicineId(1L);
        lowStock.setWarehouseId(1L);
        lowStock.setTotalStock(3L);

        when(inventoryService.getLowStockInventory()).thenReturn(List.of(lowStock));
        when(alertRepository.findActiveAlertByMedicineWarehouseAndType(1L, 1L, "LOW_STOCK"))
            .thenReturn(new ArrayList<>());
        when(alertRepository.findByAlertType("LOW_STOCK")).thenReturn(new ArrayList<>());
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert saved = invocation.getArgument(0);
            saved.setAlertId(1L);
            return saved;
        });
        when(alertHistoryRepository.save(any(AlertHistory.class))).thenReturn(testAlertHistory);

        alertService.checkLowStockAlerts();

        verify(inventoryService, times(1)).getLowStockInventory();
        verify(alertRepository, times(1)).findActiveAlertByMedicineWarehouseAndType(1L, 1L, "LOW_STOCK");
        verify(alertRepository, times(1)).save(any(Alert.class));
        verify(alertHistoryRepository, times(1)).save(any(AlertHistory.class));
    }

    @Test
    @DisplayName("Alert Generation | Test checkLowStockAlerts() - Alert already exists")
    public void testCheckLowStockAlerts_AlertExists() {
        InventoryResponse lowStock = new InventoryResponse();
        lowStock.setMedicineId(1L);
        lowStock.setWarehouseId(1L);
        lowStock.setTotalStock(8L);

        Alert existingAlert = new Alert();
        existingAlert.setAlertId(1L);
        existingAlert.setAlertType("LOW_STOCK");

        when(inventoryService.getLowStockInventory()).thenReturn(List.of(lowStock));
        when(alertRepository.findActiveAlertByMedicineWarehouseAndType(1L, 1L, "LOW_STOCK"))
            .thenReturn(List.of(existingAlert));
        when(alertRepository.findByAlertType("LOW_STOCK")).thenReturn(new ArrayList<>());

        alertService.checkLowStockAlerts();

        verify(inventoryService, times(1)).getLowStockInventory();
        verify(alertRepository, times(1)).findActiveAlertByMedicineWarehouseAndType(1L, 1L, "LOW_STOCK");
        verify(alertRepository, never()).save(any(Alert.class));
        verify(alertHistoryRepository, never()).save(any(AlertHistory.class));
    }

    @Test
    @DisplayName("Alert Generation | Test checkLowStockAlerts() - No alert needed")
    public void testCheckLowStockAlerts_NoAlertNeeded() {
        when(inventoryService.getLowStockInventory()).thenReturn(new ArrayList<>());
        when(alertRepository.findByAlertType("LOW_STOCK")).thenReturn(new ArrayList<>());

        alertService.checkLowStockAlerts();

        verify(inventoryService, times(1)).getLowStockInventory();
        verify(alertRepository, never()).findActiveAlertByMedicineWarehouseAndType(anyLong(), anyLong(), anyString());
        verify(alertRepository, never()).save(any(Alert.class));
        verify(alertHistoryRepository, never()).save(any(AlertHistory.class));
    }

    @Test
    @DisplayName("Alert Generation | Test checkExpiringBatches() - Create new alert")
    public void testCheckExpiringBatches_CreateNewAlert() {
        testBatch.setExpiryDate(LocalDate.now().plusDays(15)); // Within threshold

        when(batchRepository.findAll()).thenReturn(List.of(testBatch));
        when(alertRepository.findActiveAlertByBatchAndType(1L, "EXPIRING_SOON"))
            .thenReturn(new ArrayList<>());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert saved = invocation.getArgument(0);
            saved.setAlertId(1L);
            return saved;
        });
        when(alertHistoryRepository.save(any(AlertHistory.class))).thenReturn(testAlertHistory);

        alertService.checkExpiringBatches();

        verify(batchRepository, times(1)).findAll();
        verify(alertRepository, times(1)).findActiveAlertByBatchAndType(1L, "EXPIRING_SOON");
        verify(alertRepository, times(1)).save(any(Alert.class));
        verify(alertHistoryRepository, times(1)).save(any(AlertHistory.class));
    }

    @Test
    @DisplayName("Alert Generation | Test checkExpiringBatches() - No alert needed")
    public void testCheckExpiringBatches_NoAlertNeeded() {
        testBatch.setExpiryDate(LocalDate.now().plusDays(60)); // Beyond threshold

        when(batchRepository.findAll()).thenReturn(List.of(testBatch));

        alertService.checkExpiringBatches();

        verify(batchRepository, times(1)).findAll();
        verify(alertRepository, never()).findActiveAlertByBatchAndType(anyLong(), anyString());
        verify(alertRepository, never()).save(any(Alert.class));
        verify(alertHistoryRepository, never()).save(any(AlertHistory.class));
    }

    @Test
    @DisplayName("Alert Generation | Test checkExpiredBatches() - Create new alert")
    public void testCheckExpiredBatches_CreateNewAlert() {
        testBatch.setExpiryDate(LocalDate.now().minusDays(1)); // Expired

        when(batchRepository.findAll()).thenReturn(List.of(testBatch));
        when(alertRepository.findActiveAlertByBatchAndType(1L, "EXPIRED"))
            .thenReturn(new ArrayList<>());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert saved = invocation.getArgument(0);
            saved.setAlertId(1L);
            return saved;
        });
        when(alertHistoryRepository.save(any(AlertHistory.class))).thenReturn(testAlertHistory);

        alertService.checkExpiredBatches();

        verify(batchRepository, times(1)).findAll();
        verify(alertRepository, times(1)).findActiveAlertByBatchAndType(1L, "EXPIRED");
        verify(alertRepository, times(1)).save(any(Alert.class));
        verify(alertHistoryRepository, times(1)).save(any(AlertHistory.class));
    }

    // ==========================================
    // 4. PRIVATE METHOD TESTING
    // ==========================================

    @Test
    @DisplayName("Private Method | Test convertToResponse() with complete data")
    public void testConvertToResponse_CompleteData() throws Exception {
        testAlert.setResolvedBy(testUser);
        testAlert.setResolvedAt(testTime.plusHours(1));

        // Use reflection to test private method
        AlertResponse result = invokePrivateConvertToResponse(testAlert);

        assertNotNull(result);
        assertEquals(1L, result.getAlertId());
        assertEquals("LOW_STOCK", result.getAlertType());
        assertEquals("HIGH", result.getSeverity());
        assertEquals("OPEN", result.getStatus());
        assertEquals("Low stock alert", result.getMessage());
        assertEquals("Stock level is low", result.getDescription());
        assertEquals(testTime, result.getCreatedAt());
        assertEquals(testTime.plusHours(1), result.getResolvedAt());
        assertEquals(1L, result.getBatchId());
        assertEquals("LOT001", result.getLotNumber());
        assertEquals(1L, result.getMedicineId());
        assertEquals("Test Medicine", result.getMedicineName());
        assertEquals(1L, result.getWarehouseId());
        assertEquals("Main Warehouse", result.getWarehouseName());
        assertEquals(1L, result.getResolvedByUserId());
        assertEquals("testuser", result.getResolvedByUsername());
    }

    @Test
    @DisplayName("Private Method | Test convertToResponse() with null relationships")
    public void testConvertToResponse_NullRelationships() throws Exception {
        testAlert.setBatch(null);
        testAlert.setMedicine(null);
        testAlert.setWarehouse(null);
        testAlert.setResolvedBy(null);

        AlertResponse result = invokePrivateConvertToResponse(testAlert);

        assertNotNull(result);
        assertNull(result.getBatchId());
        assertNull(result.getLotNumber());
        assertNull(result.getMedicineId());
        assertNull(result.getMedicineName());
        assertNull(result.getWarehouseId());
        assertNull(result.getWarehouseName());
        assertNull(result.getResolvedByUserId());
        assertNull(result.getResolvedByUsername());
    }

    @Test
    @DisplayName("Private Method | Test convertHistoryToResponse() with complete data")
    public void testConvertHistoryToResponse_CompleteData() throws Exception {
        AlertHistoryResponse result = invokePrivateConvertHistoryToResponse(testAlertHistory);

        assertNotNull(result);
        assertEquals(1L, result.getHistoryId());
        assertEquals(1L, result.getAlertId());
        assertEquals("CREATED", result.getAction());
        assertNull(result.getOldStatus());
        assertEquals("OPEN", result.getNewStatus());
        assertEquals("Auto-generated alert", result.getComment());
        assertEquals(testTime, result.getTimestamp());
        assertEquals("testuser", result.getUsername());
        assertEquals("Test User", result.getUserFullName());
    }

    @Test
    @DisplayName("Private Method | Test convertHistoryToResponse() with null user")
    public void testConvertHistoryToResponse_NullUser() throws Exception {
        testAlertHistory.setUser(null);

        AlertHistoryResponse result = invokePrivateConvertHistoryToResponse(testAlertHistory);

        assertNotNull(result);
        assertNull(result.getUsername());
        assertNull(result.getUserFullName());
    }

    @Test
    @DisplayName("Private Method | Test createLowStockAlert() - CRITICAL severity")
    public void testCreateLowStockAlert_CriticalSeverity() throws Exception {
        InventoryResponse lowStock = new InventoryResponse();
        lowStock.setMedicineId(1L);
        lowStock.setWarehouseId(1L);
        lowStock.setTotalStock(3L);

        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));

        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert saved = invocation.getArgument(0);
            saved.setAlertId(1L);
            return saved;
        });
        when(alertHistoryRepository.save(any(AlertHistory.class))).thenReturn(testAlertHistory);

        invokePrivateCreateLowStockAlert(lowStock);

        verify(alertRepository).save(argThat(alert -> 
            "LOW_STOCK".equals(alert.getAlertType()) &&
            "CRITICAL".equals(alert.getSeverity()) &&
            "OPEN".equals(alert.getStatus())
        ));
        verify(alertHistoryRepository).save(any(AlertHistory.class));
    }

    @Test
    @DisplayName("Private Method | Test createLowStockAlert() - HIGH severity")
    public void testCreateLowStockAlert_HighSeverity() throws Exception {
        InventoryResponse lowStock = new InventoryResponse();
        lowStock.setMedicineId(1L);
        lowStock.setWarehouseId(1L);
        lowStock.setTotalStock(8L);

        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));

        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert saved = invocation.getArgument(0);
            saved.setAlertId(1L);
            return saved;
        });
        when(alertHistoryRepository.save(any(AlertHistory.class))).thenReturn(testAlertHistory);

        invokePrivateCreateLowStockAlert(lowStock);

        verify(alertRepository).save(argThat(alert -> 
            "LOW_STOCK".equals(alert.getAlertType()) &&
            "HIGH".equals(alert.getSeverity()) &&
            "OPEN".equals(alert.getStatus())
        ));
        verify(alertHistoryRepository).save(any(AlertHistory.class));
    }

    // ==========================================
    // 5. HISTORY TESTING
    // ==========================================

    @Test
    @DisplayName("History | Test getAlertHistory() with data")
    public void testGetAlertHistory_WithData() {
        when(alertHistoryRepository.findHistoryByAlertId(1L))
            .thenReturn(List.of(testAlertHistory));

        List<AlertHistoryResponse> result = alertService.getAlertHistory(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getHistoryId());
        assertEquals(1L, result.get(0).getAlertId());
        assertEquals("CREATED", result.get(0).getAction());
        assertEquals("testuser", result.get(0).getUsername());

        verify(alertHistoryRepository, times(1)).findHistoryByAlertId(1L);
    }

    @Test
    @DisplayName("History | Test getAlertHistory() empty")
    public void testGetAlertHistory_Empty() {
        when(alertHistoryRepository.findHistoryByAlertId(1L))
            .thenReturn(new ArrayList<>());

        List<AlertHistoryResponse> result = alertService.getAlertHistory(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(alertHistoryRepository, times(1)).findHistoryByAlertId(1L);
    }

    // ==========================================
    // 6. SUPPLEMENTARY COVERAGE TESTS
    // ==========================================

    @Test
    @DisplayName("Supplementary: Test alert generation with null values")
    public void testAlertGeneration_NullValues() {
        when(inventoryService.getLowStockInventory()).thenReturn(new ArrayList<>());
        when(alertRepository.findByAlertType("LOW_STOCK")).thenReturn(new ArrayList<>());

        alertService.checkLowStockAlerts();
        verify(inventoryService, times(1)).getLowStockInventory();

        verify(alertRepository, never()).findActiveAlertByMedicineWarehouseAndType(anyLong(), anyLong(), anyString());

        // Test batch with null expiry date
        testBatch.setExpiryDate(null);
        when(batchRepository.findAll()).thenReturn(List.of(testBatch));

        alertService.checkExpiringBatches();
        alertService.checkExpiredBatches();

        verify(alertRepository, never()).findActiveAlertByBatchAndType(anyLong(), eq("EXPIRING_SOON"));
        verify(alertRepository, never()).findActiveAlertByBatchAndType(anyLong(), eq("EXPIRED"));
    }

    // ==========================================
    // 7. HELPER METHODS FOR TESTING PRIVATE METHODS
    // ==========================================

    private AlertResponse invokePrivateConvertToResponse(Alert alert) {
        try {
            java.lang.reflect.Method method = AlertService.class.getDeclaredMethod("convertToResponse", Alert.class);
            method.setAccessible(true);
            return (AlertResponse) method.invoke(alertService, alert);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke private method", e);
        }
    }

    private AlertHistoryResponse invokePrivateConvertHistoryToResponse(AlertHistory history) {
        try {
            java.lang.reflect.Method method = AlertService.class.getDeclaredMethod("convertHistoryToResponse", AlertHistory.class);
            method.setAccessible(true);
            return (AlertHistoryResponse) method.invoke(alertService, history);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke private method", e);
        }
    }

    private void invokePrivateCreateLowStockAlert(InventoryResponse inventory) {
        try {
            java.lang.reflect.Method method = AlertService.class.getDeclaredMethod("createLowStockAlert", InventoryResponse.class);
            method.setAccessible(true);
            method.invoke(alertService, inventory);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke private method", e);
        }
    }

    private void invokePrivateCreateExpiringSoonAlert(Batch batch) {
        try {
            java.lang.reflect.Method method = AlertService.class.getDeclaredMethod("createExpiringSoonAlert", Batch.class);
            method.setAccessible(true);
            method.invoke(alertService, batch);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke private method", e);
        }
    }

    private void invokePrivateCreateExpiredAlert(Batch batch) {
        try {
            java.lang.reflect.Method method = AlertService.class.getDeclaredMethod("createExpiredAlert", Batch.class);
            method.setAccessible(true);
            method.invoke(alertService, batch);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke private method", e);
        }
    }
}
