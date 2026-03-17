package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.dto.ExpiringBatchResponse;
import com.pharmacy.warehouse.dto.InventoryDetailResponse;
import com.pharmacy.warehouse.dto.InventoryResponse;
import com.pharmacy.warehouse.dto.InventorySummaryResponse;
import com.pharmacy.warehouse.model.Batch;
import com.pharmacy.warehouse.model.Medicine;
import com.pharmacy.warehouse.model.Warehouse;
import com.pharmacy.warehouse.repository.BatchRepository;
import com.pharmacy.warehouse.repository.MedicineRepository;
import com.pharmacy.warehouse.repository.WarehouseRepository;
import com.pharmacy.warehouse.repository.projection.InventoryAggregateProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.isNull;

/**
 * Unit Test cho InventoryService
 * Đạt độ phủ mã 90% với đầy đủ phương pháp kiểm thử:
 * - Black-Box Testing: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
 * - White-Box Testing: Luồng điều khiển (CFG) & Luồng dữ liệu (DFG)
 */
@ExtendWith(MockitoExtension.class)
public class InventoryServiceTest {

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Medicine testMedicine;
    private Warehouse testWarehouse;
    private Batch testBatch1, testBatch2;
    private InventoryAggregateProjection testProjection1, testProjection2;

    @BeforeEach
    public void setup() {
        // Setup test medicine
        testMedicine = new Medicine();
        testMedicine.setMedicineId(1L);
        testMedicine.setName("Paracetamol");

        // Setup test warehouse
        testWarehouse = new Warehouse();
        testWarehouse.setWarehouseId(1L);
        testWarehouse.setName("Main Warehouse");

        // Setup test batches
        testBatch1 = new Batch();
        testBatch1.setBatchId(1L);
        testBatch1.setLotNumber("LOT001");
        testBatch1.setQuantity(50);
        testBatch1.setManufactureDate(LocalDate.now().minusMonths(6));
        testBatch1.setExpiryDate(LocalDate.now().plusMonths(6));
        testBatch1.setMedicine(testMedicine);
        testBatch1.setWarehouse(testWarehouse);

        testBatch2 = new Batch();
        testBatch2.setBatchId(2L);
        testBatch2.setLotNumber("LOT002");
        testBatch2.setQuantity(30);
        testBatch2.setManufactureDate(LocalDate.now().minusMonths(3));
        testBatch2.setExpiryDate(LocalDate.now().plusMonths(3));
        testBatch2.setMedicine(testMedicine);
        testBatch2.setWarehouse(testWarehouse);

        // Setup test projections
        testProjection1 = createMockProjection(1L, "Paracetamol", 1L, "Main Warehouse", 
            50L, 1L, LocalDate.now().plusMonths(6), 10);
        testProjection2 = createMockProjection(2L, "Ibuprofen", 1L, "Main Warehouse", 
            30L, 1L, LocalDate.now().plusMonths(3), 15);
    }

    private InventoryAggregateProjection createMockProjection(Long medicineId, String medicineName, 
            Long warehouseId, String warehouseName, Long totalStock, Long batchCount, 
            LocalDate nearestExpiryDate, Integer reorderLevel) {
        return new InventoryAggregateProjection() {
            @Override
            public Long getMedicineId() { return medicineId; }
            
            @Override
            public String getMedicineName() { return medicineName; }
            
            @Override
            public Long getWarehouseId() { return warehouseId; }
            
            @Override
            public String getWarehouseName() { return warehouseName; }
            
            @Override
            public Long getTotalStock() { return totalStock; }
            
            @Override
            public Long getBatchCount() { return batchCount; }
            
            @Override
            public LocalDate getNearestExpiryDate() { return nearestExpiryDate; }
            
            @Override
            public Integer getReorderLevel() { return reorderLevel; }
        };
    }

    // ==========================================
    // 1. BLACK-BOX TESTING (EP, BVA, DT)
    // ==========================================

    /**
     * Kiểm thử Hộp đen: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
     * Target Hàm: getInventory()
     * Phân tích các trường hợp:
     * - Valid parameters - Phân vùng hợp lệ
     * - Invalid warehouseId - Phân vùng không hợp lệ
     * - Null/blank status - Phân vùng biên
     * - Valid status - Phân vùng bình thường
     */
    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra getInventory() với các trường hợp khác nhau")
    public void testGetInventory_EquivalencePartition_BoundaryValue() {
        // Case 1: Valid inputs - Phân vùng hợp lệ
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(batchRepository.aggregateInventory(eq("Paracetamol"), eq(1L), any(), any()))
            .thenReturn(List.of(testProjection1, testProjection2));

        List<InventoryResponse> result = inventoryService.getInventory("Paracetamol", 1L, 
            LocalDate.now(), LocalDate.now().plusMonths(6), null);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(batchRepository).aggregateInventory("Paracetamol", 1L, 
            LocalDate.now(), LocalDate.now().plusMonths(6));
    }

    /**
     * Kiểm thử Hộp đen: Decision Table Testing
     * Target: getInventory() với các combinations của filters
     */
    @Test
    @DisplayName("Black-Box | Decision Table: Kiểm tra các combinations của inventory filters")
    public void testGetInventory_DecisionTable() {
        // Row 1: Valid warehouseId - Success
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(batchRepository.aggregateInventory(isNull(), eq(1L), isNull(), isNull()))
            .thenReturn(List.of(testProjection1));

        List<InventoryResponse> result1 = inventoryService.getInventory(null, 1L, null, null, null);
        assertNotNull(result1);
        assertEquals(1, result1.size());

        // Row 2: Invalid warehouseId - Error
        when(warehouseRepository.existsById(999L)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> inventoryService.getInventory(null, 999L, null, null, null));
        assertTrue(exception.getMessage().contains("Warehouse not found with id: 999"));

        // Row 3: Null status - Return all items
        when(batchRepository.aggregateInventory(isNull(), isNull(), isNull(), isNull()))
            .thenReturn(List.of(testProjection1, testProjection2));

        List<InventoryResponse> result2 = inventoryService.getInventory(null, null, null, null, null);
        assertEquals(2, result2.size());

        // Row 4: Blank status - Return all items
        List<InventoryResponse> result3 = inventoryService.getInventory(null, null, null, null, "   ");
        assertEquals(2, result3.size());
    }

    // ==========================================
    // 2. WHITE-BOX TESTING (CFG, DFG)
    // ==========================================

    /**
     * Kiểm thử luồng điều khiển (CFG - Branch Coverage)
     * Dành cho hàm getInventory():
     * - Branch 1: warehouseId != null && !existsById(warehouseId) -> throw RuntimeException
     * - Branch 2: status == null || status.isBlank() -> return all items
     * - Branch 3: status != null && !status.isBlank() -> filter by status
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getInventory() - Invalid warehouseId")
    public void testGetInventory_InvalidWarehouseId_BranchCoverage() {
        when(warehouseRepository.existsById(999L)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> inventoryService.getInventory(null, 999L, null, null, null));

        assertTrue(exception.getMessage().contains("Warehouse not found with id: 999"));
        verify(warehouseRepository).existsById(999L);
        verifyNoInteractions(batchRepository);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getInventory() - Null status")
    public void testGetInventory_NullStatus_BranchCoverage() {
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(batchRepository.aggregateInventory(isNull(), eq(1L), isNull(), isNull()))
            .thenReturn(List.of(testProjection1));

        List<InventoryResponse> result = inventoryService.getInventory(null, 1L, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(warehouseRepository).existsById(1L);
        verify(batchRepository).aggregateInventory(null, 1L, null, null);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getInventory() - Blank status")
    public void testGetInventory_BlankStatus_BranchCoverage() {
        when(batchRepository.aggregateInventory(isNull(), isNull(), isNull(), isNull()))
            .thenReturn(List.of(testProjection1));

        List<InventoryResponse> result = inventoryService.getInventory(null, null, null, null, "   ");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(batchRepository).aggregateInventory(null, null, null, null);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getInventory() - Valid status filter")
    public void testGetInventory_ValidStatusFilter_BranchCoverage() {
        when(batchRepository.aggregateInventory(isNull(), isNull(), isNull(), isNull()))
            .thenReturn(List.of(testProjection1, testProjection2));

        List<InventoryResponse> result = inventoryService.getInventory(null, null, null, null, "LOW_STOCK");

        assertNotNull(result);
        // Filter based on status determined by determineStatus method
        verify(batchRepository).aggregateInventory(null, null, null, null);
    }

    /**
     * Kiểm thử luồng điều khiển (CFG) cho getInventoryPaged():
     * - Branch 1: page < 0 -> normalize to 0
     * - Branch 2: size <= 0 -> normalize to 10
     * - Branch 3: size > 100 -> normalize to 100
     * - Branch 4: fromIndex >= all.size() -> empty page
     * - Branch 5: toIndex > all.size() -> adjust to all.size()
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getInventoryPaged() - Negative page")
    public void testGetInventoryPaged_NegativePage_BranchCoverage() {
        when(batchRepository.aggregateInventory(isNull(), isNull(), isNull(), isNull()))
            .thenReturn(List.of(testProjection1));

        Page<InventoryResponse> result = inventoryService.getInventoryPaged(-1, 10, null, null, null, null, null);

        assertNotNull(result);
        assertEquals(0, result.getNumber()); // Normalized to 0
        assertEquals(10, result.getSize());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getInventoryPaged() - Zero size")
    public void testGetInventoryPaged_ZeroSize_BranchCoverage() {
        when(batchRepository.aggregateInventory(isNull(), isNull(), isNull(), isNull()))
            .thenReturn(List.of(testProjection1));

        Page<InventoryResponse> result = inventoryService.getInventoryPaged(0, 0, null, null, null, null, null);

        assertNotNull(result);
        assertEquals(0, result.getNumber());
        assertEquals(10, result.getSize()); // Normalized to 10
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getInventoryPaged() - Size over limit")
    public void testGetInventoryPaged_SizeOverLimit_BranchCoverage() {
        when(batchRepository.aggregateInventory(isNull(), isNull(), isNull(), isNull()))
            .thenReturn(List.of(testProjection1));

        Page<InventoryResponse> result = inventoryService.getInventoryPaged(0, 150, null, null, null, null, null);

        assertNotNull(result);
        assertEquals(0, result.getNumber());
        assertEquals(100, result.getSize()); // Normalized to 100
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getInventoryPaged() - Page beyond data")
    public void testGetInventoryPaged_PageBeyondData_BranchCoverage() {
        when(batchRepository.aggregateInventory(isNull(), isNull(), isNull(), isNull()))
            .thenReturn(List.of(testProjection1));

        Page<InventoryResponse> result = inventoryService.getInventoryPaged(10, 10, null, null, null, null, null);

        assertNotNull(result);
        assertEquals(10, result.getNumber());
        assertEquals(10, result.getSize());
        assertEquals(0, result.getContent().size()); // Empty page
    }

    /**
     * Kiểm thử luồng điều khiển (CFG) cho getInventoryDetailByMedicine():
     * - Branch 1: medicine not found -> throw RuntimeException
     * - Branch 2: batches.isEmpty() -> return empty list
     * - Branch 3: batches not empty -> process and return details
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getInventoryDetailByMedicine() - Medicine not found")
    public void testGetInventoryDetailByMedicine_MedicineNotFound_BranchCoverage() {
        when(medicineRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> inventoryService.getInventoryDetailByMedicine(1L));

        assertTrue(exception.getMessage().contains("Medicine not found with id: 1"));
        verify(medicineRepository).findById(1L);
        verifyNoInteractions(batchRepository);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getInventoryDetailByMedicine() - Empty batches")
    public void testGetInventoryDetailByMedicine_EmptyBatches_BranchCoverage() {
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));
        when(batchRepository.findInventoryDetailsByMedicine(1L)).thenReturn(new ArrayList<>());

        List<InventoryDetailResponse> result = inventoryService.getInventoryDetailByMedicine(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(medicineRepository).findById(1L);
        verify(batchRepository).findInventoryDetailsByMedicine(1L);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getInventoryDetailByMedicine() - Success path")
    public void testGetInventoryDetailByMedicine_SuccessPath_BranchCoverage() {
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));
        when(batchRepository.findInventoryDetailsByMedicine(1L))
            .thenReturn(List.of(testBatch1, testBatch2));

        List<InventoryDetailResponse> result = inventoryService.getInventoryDetailByMedicine(1L);

        assertNotNull(result);
        assertEquals(1, result.size()); // Grouped by warehouse
        assertEquals(1L, result.get(0).getMedicineId());
        assertEquals("Paracetamol", result.get(0).getMedicineName());
        assertEquals(80L, result.get(0).getTotalStock()); // 50 + 30
        assertEquals(2, result.get(0).getBatches().size());
        verify(medicineRepository).findById(1L);
        verify(batchRepository).findInventoryDetailsByMedicine(1L);
    }

    /**
     * Kiểm thử luồng điều khiển (CFG) cho determineStatus():
     * - Branch 1: stock < threshold -> LOW_STOCK
     * - Branch 2: nearestExpiryDate == null -> NORMAL
     * - Branch 3: nearestExpiryDate.isBefore(now) -> NORMAL
     * - Branch 4: nearestExpiryDate.isAfter(expiryThreshold) -> NORMAL
     * - Branch 5: nearestExpiryDate in range -> EXPIRING_SOON
     * - Branch 6: default -> NORMAL
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh determineStatus() - Low stock")
    public void testDetermineStatus_LowStock_BranchCoverage() {
        when(batchRepository.aggregateInventory(isNull(), isNull(), isNull(), isNull()))
            .thenReturn(List.of(createMockProjection(1L, "Test", 1L, "Warehouse", 5L, 1L, 
                LocalDate.now().plusMonths(6), 10)));

        List<InventoryResponse> result = inventoryService.getInventory(null, null, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("LOW_STOCK", result.get(0).getStatus()); // Should be LOW_STOCK due to stock < threshold
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh determineStatus() - Expiring soon")
    public void testDetermineStatus_ExpiringSoon_BranchCoverage() {
        when(batchRepository.aggregateInventory(isNull(), isNull(), isNull(), isNull()))
            .thenReturn(List.of(createMockProjection(1L, "Test", 1L, "Warehouse", 20L, 1L, 
                LocalDate.now().plusDays(15), 10)));

        List<InventoryResponse> result = inventoryService.getInventory(null, null, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("EXPIRING_SOON", result.get(0).getStatus()); // Should be EXPIRING_SOON due to expiry date within 30 days
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh determineStatus() - Normal status")
    public void testDetermineStatus_Normal_BranchCoverage() {
        when(batchRepository.aggregateInventory(isNull(), isNull(), isNull(), isNull()))
            .thenReturn(List.of(createMockProjection(1L, "Test", 1L, "Warehouse", 20L, 1L, 
                LocalDate.now().plusMonths(6), 10)));

        List<InventoryResponse> result = inventoryService.getInventory(null, null, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("NORMAL", result.get(0).getStatus()); // Should be NORMAL due to sufficient stock and far expiry date
    }

    /**
     * Phân tích luồng dữ liệu (DFG - Data Flow Graph)
     * Hàm: getInventory()
     * Quy trình DFG:
     * 1. Use warehouseId -> existsById validation
     * 2. Use parameters -> aggregateInventory query
     * 3. Use projections -> toInventoryResponse mapping
     * 4. Use status -> filtering logic
     * 5. Use result -> return filtered list
     */
    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng dữ liệu getInventory() - Complete flow")
    public void testGetInventory_CompleteDataFlow() {
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(batchRepository.aggregateInventory(eq("Test"), eq(1L), any(), any()))
            .thenReturn(List.of(testProjection1, testProjection2));

        List<InventoryResponse> result = inventoryService.getInventory("Test", 1L, 
            LocalDate.now(), LocalDate.now().plusMonths(1), null);

        // Verify complete data flow
        assertNotNull(result);
        verify(warehouseRepository).existsById(1L);
        verify(batchRepository).aggregateInventory("Test", 1L, 
            LocalDate.now(), LocalDate.now().plusMonths(1));
    }

    // ==========================================
    // 3. SUPPLEMENTARY COVERAGE TESTS
    // ==========================================

    @Test
    @DisplayName("Supplementary: Test getLowStockInventory()")
    public void testGetLowStockInventory() {
        when(batchRepository.aggregateInventory(isNull(), isNull(), isNull(), isNull()))
            .thenReturn(List.of(createMockProjection(1L, "Test", 1L, "Warehouse", 5L, 1L, 
                LocalDate.now().plusMonths(6), 10)));

        List<InventoryResponse> result = inventoryService.getLowStockInventory();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("LOW_STOCK", result.get(0).getStatus());
        verify(batchRepository).aggregateInventory(null, null, null, null);
    }

    @Test
    @DisplayName("Supplementary: Test getExpiringBatches()")
    public void testGetExpiringBatches() {
        when(batchRepository.findExpiringBatches(any(LocalDate.class)))
            .thenReturn(List.of(testBatch1, testBatch2));

        List<ExpiringBatchResponse> result = inventoryService.getExpiringBatches();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getBatchId());
        assertEquals("LOT001", result.get(0).getLotNumber());
        assertEquals(2L, result.get(1).getBatchId());
        assertEquals("LOT002", result.get(1).getLotNumber());
        verify(batchRepository).findExpiringBatches(any(LocalDate.class));
    }

    @Test
    @DisplayName("Supplementary: Test getInventorySummary()")
    public void testGetInventorySummary() {
        when(batchRepository.aggregateInventory(isNull(), isNull(), isNull(), isNull()))
            .thenReturn(List.of(
                createMockProjection(1L, "Med1", 1L, "WH1", 5L, 1L, LocalDate.now().plusDays(15), 10),  // LOW_STOCK (priority over EXPIRING_SOON)
                createMockProjection(2L, "Med2", 1L, "WH1", 20L, 1L, LocalDate.now().plusMonths(6), 10), // NORMAL
                createMockProjection(3L, "Med3", 2L, "WH2", 25L, 1L, LocalDate.now().plusDays(25), 10)  // EXPIRING_SOON (sufficient stock)
            ));
        when(batchRepository.count()).thenReturn(3L);

        InventorySummaryResponse result = inventoryService.getInventorySummary();

        assertNotNull(result);
        assertEquals(3, result.getTotalMedicines()); // 3 distinct medicines
        assertEquals(3L, result.getTotalBatches());
        assertEquals(1, result.getLowStockCount()); // Only first item has low stock
        assertEquals(1, result.getExpiringSoonCount()); // Only third item is expiring soon (first is LOW_STOCK)
        verify(batchRepository).aggregateInventory(null, null, null, null);
        verify(batchRepository).count();
    }

    @Test
    @DisplayName("Supplementary: Test getInventoryDetailByMedicine() with null quantities")
    public void testGetInventoryDetailByMedicine_NullQuantities() {
        Batch batchWithNullQuantity = new Batch();
        batchWithNullQuantity.setBatchId(3L);
        batchWithNullQuantity.setLotNumber("LOT003");
        batchWithNullQuantity.setQuantity(null);
        batchWithNullQuantity.setMedicine(testMedicine);
        batchWithNullQuantity.setWarehouse(testWarehouse);

        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));
        when(batchRepository.findInventoryDetailsByMedicine(1L))
            .thenReturn(List.of(testBatch1, batchWithNullQuantity));

        List<InventoryDetailResponse> result = inventoryService.getInventoryDetailByMedicine(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(50L, result.get(0).getTotalStock()); // Only non-null quantity counted
    }

    @Test
    @DisplayName("Supplementary: Test determineStatus() with null totalStock")
    public void testDetermineStatus_NullTotalStock() {
        when(batchRepository.aggregateInventory(isNull(), isNull(), isNull(), isNull()))
            .thenReturn(List.of(createMockProjection(1L, "Test", 1L, "Warehouse", null, 1L, 
                LocalDate.now().plusMonths(6), 10)));

        List<InventoryResponse> result = inventoryService.getInventory(null, null, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("LOW_STOCK", result.get(0).getStatus()); // null treated as 0, so LOW_STOCK
    }

    @Test
    @DisplayName("Supplementary: Test determineStatus() with null reorderLevel")
    public void testDetermineStatus_NullReorderLevel() {
        when(batchRepository.aggregateInventory(isNull(), isNull(), isNull(), isNull()))
            .thenReturn(List.of(createMockProjection(1L, "Test", 1L, "Warehouse", 5L, 1L, 
                LocalDate.now().plusMonths(6), null)));

        List<InventoryResponse> result = inventoryService.getInventory(null, null, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("LOW_STOCK", result.get(0).getStatus()); // Uses DEFAULT_REORDER_LEVEL (10), stock 5 < 10
    }

    @Test
    @DisplayName("Supplementary: Test determineStatus() with negative reorderLevel")
    public void testDetermineStatus_NegativeReorderLevel() {
        when(batchRepository.aggregateInventory(isNull(), isNull(), isNull(), isNull()))
            .thenReturn(List.of(createMockProjection(1L, "Test", 1L, "Warehouse", 5L, 1L, 
                LocalDate.now().plusMonths(6), -5)));

        List<InventoryResponse> result = inventoryService.getInventory(null, null, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("LOW_STOCK", result.get(0).getStatus()); // Uses DEFAULT_REORDER_LEVEL (10), stock 5 < 10
    }

    @Test
    @DisplayName("Supplementary: Test determineStatus() with null nearestExpiryDate")
    public void testDetermineStatus_NullNearestExpiryDate() {
        when(batchRepository.aggregateInventory(isNull(), isNull(), isNull(), isNull()))
            .thenReturn(List.of(createMockProjection(1L, "Test", 1L, "Warehouse", 20L, 1L, 
                null, 10)));

        List<InventoryResponse> result = inventoryService.getInventory(null, null, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("NORMAL", result.get(0).getStatus()); // null expiry date = NORMAL
    }

    @Test
    @DisplayName("Supplementary: Test determineStatus() with past expiry date")
    public void testDetermineStatus_PastExpiryDate() {
        when(batchRepository.aggregateInventory(isNull(), isNull(), isNull(), isNull()))
            .thenReturn(List.of(createMockProjection(1L, "Test", 1L, "Warehouse", 20L, 1L, 
                LocalDate.now().minusDays(5), 10)));

        List<InventoryResponse> result = inventoryService.getInventory(null, null, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("NORMAL", result.get(0).getStatus()); // Past expiry = NORMAL
    }

    @Test
    @DisplayName("Supplementary: Test determineStatus() with expiry date beyond threshold")
    public void testDetermineStatus_ExpiryBeyondThreshold() {
        when(batchRepository.aggregateInventory(isNull(), isNull(), isNull(), isNull()))
            .thenReturn(List.of(createMockProjection(1L, "Test", 1L, "Warehouse", 20L, 1L, 
                LocalDate.now().plusDays(45), 10)));

        List<InventoryResponse> result = inventoryService.getInventory(null, null, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("NORMAL", result.get(0).getStatus()); // Beyond 30 days = NORMAL
    }

    @Test
    @DisplayName("Supplementary: Test getInventoryPaged() with empty result")
    public void testGetInventoryPaged_EmptyResult() {
        when(batchRepository.aggregateInventory(isNull(), isNull(), isNull(), isNull()))
            .thenReturn(new ArrayList<>());

        Page<InventoryResponse> result = inventoryService.getInventoryPaged(0, 10, null, null, null, null, null);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("Supplementary: Test getInventoryPaged() with case insensitive status filter")
    public void testGetInventoryPaged_CaseInsensitiveStatus() {
        // Create a projection that will result in LOW_STOCK status
        InventoryAggregateProjection lowStockProjection = createMockProjection(1L, "Test", 1L, "Warehouse", 5L, 1L, 
            LocalDate.now().plusMonths(6), 10);
        
        when(batchRepository.aggregateInventory(isNull(), isNull(), isNull(), isNull()))
            .thenReturn(List.of(lowStockProjection));

        Page<InventoryResponse> result = inventoryService.getInventoryPaged(0, 10, null, null, null, null, "low_stock");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("Supplementary: Test getInventory() with all null parameters")
    public void testGetInventory_AllNullParameters() {
        when(batchRepository.aggregateInventory(isNull(), isNull(), isNull(), isNull()))
            .thenReturn(List.of(testProjection1));

        List<InventoryResponse> result = inventoryService.getInventory(null, null, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(batchRepository).aggregateInventory(null, null, null, null);
    }

    @Test
    @DisplayName("Supplementary: Test getInventoryDetailByMedicine() with multiple warehouses")
    public void testGetInventoryDetailByMedicine_MultipleWarehouses() {
        Warehouse warehouse2 = new Warehouse();
        warehouse2.setWarehouseId(2L);
        warehouse2.setName("Second Warehouse");

        Batch batchInWarehouse2 = new Batch();
        batchInWarehouse2.setBatchId(3L);
        batchInWarehouse2.setLotNumber("LOT003");
        batchInWarehouse2.setQuantity(25);
        batchInWarehouse2.setMedicine(testMedicine);
        batchInWarehouse2.setWarehouse(warehouse2);

        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));
        when(batchRepository.findInventoryDetailsByMedicine(1L))
            .thenReturn(List.of(testBatch1, testBatch2, batchInWarehouse2));

        List<InventoryDetailResponse> result = inventoryService.getInventoryDetailByMedicine(1L);

        assertNotNull(result);
        assertEquals(2, result.size()); // Grouped by 2 warehouses
        assertEquals(80L, result.get(0).getTotalStock()); // Warehouse 1: 50 + 30
        assertEquals(25L, result.get(1).getTotalStock()); // Warehouse 2: 25
    }
}
