package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.model.Batch;
import com.pharmacy.warehouse.model.Medicine;
import com.pharmacy.warehouse.model.Warehouse;
import com.pharmacy.warehouse.repository.BatchRepository;
import com.pharmacy.warehouse.repository.MedicineRepository;
import com.pharmacy.warehouse.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BatchServiceTest {

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private BatchService batchService;

    private Batch batchMock;
    private Medicine medicineMock;
    private Warehouse warehouseMock;

    @BeforeEach
    public void setup() {
        medicineMock = new Medicine();
        medicineMock.setMedicineId(1L);
        medicineMock.setName("Paracetamol");
        medicineMock.setManufacturer("Pfizer");
        medicineMock.setStorageCondition("Normal");

        warehouseMock = new Warehouse();
        warehouseMock.setWarehouseId(1L);
        warehouseMock.setName("Main Warehouse");
        warehouseMock.setLocation("HCM City");
        warehouseMock.setDescription("Primary storage facility");

        batchMock = new Batch();
        batchMock.setBatchId(1L);
        batchMock.setLotNumber("LOT001");
        batchMock.setManufactureDate(LocalDate.of(2023, 1, 1));
        batchMock.setExpiryDate(LocalDate.of(2025, 12, 31));
        batchMock.setQuantity(100);
        batchMock.setStatus("ACTIVE");
        batchMock.setMedicine(medicineMock);
        batchMock.setWarehouse(warehouseMock);
    }

    // ==========================================
    // 1. BLACK-BOX TESTING (EP, BVA)
    // ==========================================

    /**
     * Kiểm thử Hộp đen: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
     * Target Hàm: getAll()
     * Phân tích kết quả trả về từ repository:
     * - Empty list -> trả về empty list
     * - Single item list -> trả về single item list  
     * - Multiple items list -> trả về multiple items list
     */
    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra getAll() với các trường hợp khác nhau")
    public void testGetAll_EquivalencePartition_BoundaryValue() {
        // Case 1: Empty list - Phân vùng rỗng
        when(batchRepository.findAll()).thenReturn(new ArrayList<>());
        List<Batch> result1 = batchService.getAll();
        assertTrue(result1.isEmpty());
        verify(batchRepository, times(1)).findAll();

        // Case 2: Single item - Giá trị biên dưới
        when(batchRepository.findAll()).thenReturn(List.of(batchMock));
        List<Batch> result2 = batchService.getAll();
        assertEquals(1, result2.size());
        assertEquals("LOT001", result2.get(0).getLotNumber());
        verify(batchRepository, times(2)).findAll();

        // Case 3: Multiple items - Phân vùng bình thường
        Batch batch2 = new Batch();
        batch2.setBatchId(2L);
        batch2.setLotNumber("LOT002");
        batch2.setMedicine(medicineMock);
        
        when(batchRepository.findAll()).thenReturn(List.of(batchMock, batch2));
        List<Batch> result3 = batchService.getAll();
        assertEquals(2, result3.size());
        verify(batchRepository, times(3)).findAll();
    }

    /**
     * Kiểm thử Hộp đen: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
     * Target Hàm: getByMedicine(Long medicineId)
     * Phân tích medicineId:
     * - EP1: Valid medicineId -> trả về list batches
     * - EP2: Invalid medicineId -> trả về empty list
     * - BVA: medicineId = null, negative, zero, positive
     */
    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra getByMedicine() với các medicineId khác nhau")
    public void testGetByMedicine_EquivalencePartition_BoundaryValue() {
        // Case 1: Valid medicineId with batches
        when(batchRepository.findByMedicine_MedicineId(1L)).thenReturn(List.of(batchMock));
        List<Batch> result1 = batchService.getByMedicine(1L);
        assertEquals(1, result1.size());
        assertEquals("LOT001", result1.get(0).getLotNumber());
        verify(batchRepository, times(1)).findByMedicine_MedicineId(1L);

        // Case 2: Valid medicineId with no batches
        when(batchRepository.findByMedicine_MedicineId(2L)).thenReturn(new ArrayList<>());
        List<Batch> result2 = batchService.getByMedicine(2L);
        assertTrue(result2.isEmpty());
        verify(batchRepository, times(1)).findByMedicine_MedicineId(2L);

        // Case 3: Zero medicineId
        when(batchRepository.findByMedicine_MedicineId(0L)).thenReturn(new ArrayList<>());
        List<Batch> result3 = batchService.getByMedicine(0L);
        assertTrue(result3.isEmpty());
        verify(batchRepository, times(1)).findByMedicine_MedicineId(0L);
    }

    // ==========================================
    // 2. WHITE-BOX TESTING (CFG, DFG)
    // ==========================================

    /**
     * Kiểm thử luồng điều khiển (CFG - Branch Coverage)
     * Dành cho hàm create(Long medicineId, Batch batch):
     * - Branch 1: medicineRepository.findById() trả về Optional.isPresent() -> tiếp tục xử lý
     * - Branch 2: medicineRepository.findById() trả về Optional.isEmpty() -> ném RuntimeException
     * - Branch 3: applyWarehouseIfPresent() - batch.getWarehouse() == null -> return
     * - Branch 4: applyWarehouseIfPresent() - batch.getWarehouse() != null -> setWarehouse()
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh create() - Success path")
    public void testCreate_SuccessPath_BranchCoverage() {
        Batch newBatch = new Batch();
        newBatch.setLotNumber("NEWLOT");
        newBatch.setQuantity(50);
        newBatch.setWarehouse(warehouseMock);

        when(medicineRepository.findById(1L)).thenReturn(Optional.of(medicineMock));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouseMock));
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Batch result = batchService.create(1L, newBatch);

        assertNotNull(result);
        assertEquals("NEWLOT", result.getLotNumber());
        assertEquals(50, result.getQuantity());
        assertEquals(medicineMock, result.getMedicine());
        assertEquals(warehouseMock, result.getWarehouse());

        verify(medicineRepository, times(1)).findById(1L);
        verify(warehouseRepository, times(1)).findById(1L);
        verify(batchRepository, times(1)).save(any(Batch.class));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh create() - Medicine not found")
    public void testCreate_MedicineNotFound_BranchCoverage() {
        Batch newBatch = new Batch();
        newBatch.setLotNumber("NEWLOT");

        when(medicineRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            batchService.create(999L, newBatch);
        });

        assertEquals("Medicine not found", exception.getMessage());
        verify(medicineRepository, times(1)).findById(999L);
        verify(batchRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh create() - Batch with null warehouse")
    public void testCreate_NullWarehouse_BranchCoverage() {
        Batch newBatch = new Batch();
        newBatch.setLotNumber("NEWLOT");
        newBatch.setQuantity(50);
        newBatch.setWarehouse(null); // Null warehouse

        when(medicineRepository.findById(1L)).thenReturn(Optional.of(medicineMock));
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Batch result = batchService.create(1L, newBatch);

        assertNotNull(result);
        assertEquals("NEWLOT", result.getLotNumber());
        assertEquals(medicineMock, result.getMedicine());
        assertNull(result.getWarehouse()); // Should remain null

        verify(medicineRepository, times(1)).findById(1L);
        verify(warehouseRepository, never()).findById(any());
        verify(batchRepository, times(1)).save(any(Batch.class));
    }

    /**
     * Phân tích luồng dữ liệu (DFG - Data Flow Graph)
     * Hàm: update(Long id, Batch data)
     * Quy trình DFG:
     * 1. Define `id` -> Use trong batchRepository.findById() -> Define `batch`
     * 2. Define `data` -> Use data.getXXX() -> Use trong conditional checks
     * 3. Use `batch` -> Use trong setter methods -> Use trong batchRepository.save()
     */
    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng dữ liệu update() đầy đủ")
    public void testUpdate_CompleteDataFlow() {
        Batch updateData = new Batch();
        updateData.setLotNumber("UPDATEDLOT");
        updateData.setManufactureDate(LocalDate.of(2023, 6, 1));
        updateData.setExpiryDate(LocalDate.of(2026, 12, 31));
        updateData.setQuantity(200);
        updateData.setStatus("UPDATED");
        updateData.setWarehouse(warehouseMock);

        when(batchRepository.findById(1L)).thenReturn(Optional.of(batchMock));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouseMock));
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Batch result = batchService.update(1L, updateData);

        assertNotNull(result);
        assertEquals("UPDATEDLOT", result.getLotNumber());
        assertEquals(LocalDate.of(2023, 6, 1), result.getManufactureDate());
        assertEquals(LocalDate.of(2026, 12, 31), result.getExpiryDate());
        assertEquals(200, result.getQuantity());
        assertEquals("UPDATED", result.getStatus());
        assertEquals(warehouseMock, result.getWarehouse());

        verify(batchRepository, times(1)).findById(1L);
        verify(warehouseRepository, times(1)).findById(1L);
        verify(batchRepository, times(1)).save(any(Batch.class));
    }

    /**
     * Kiểm thử luồng điều khiển cho update()
     * Branch coverage cho tất cả các conditional checks
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh update() - Partial updates")
    public void testUpdate_PartialUpdates_BranchCoverage() {
        Batch updateData = new Batch();
        // Chỉ cập nhật lotNumber, các field khác null
        updateData.setLotNumber("PARTIAL");

        when(batchRepository.findById(1L)).thenReturn(Optional.of(batchMock));
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Batch result = batchService.update(1L, updateData);

        assertNotNull(result);
        assertEquals("PARTIAL", result.getLotNumber());
        // Các field khác không được thay đổi
        assertEquals(LocalDate.of(2023, 1, 1), result.getManufactureDate());
        assertEquals(LocalDate.of(2025, 12, 31), result.getExpiryDate());
        assertEquals(100, result.getQuantity());
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(warehouseMock, result.getWarehouse());

        verify(batchRepository, times(1)).findById(1L);
        verify(batchRepository, times(1)).save(any(Batch.class));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh update() - Batch not found")
    public void testUpdate_BatchNotFound_BranchCoverage() {
        Batch updateData = new Batch();
        updateData.setLotNumber("UPDATED");

        when(batchRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            batchService.update(999L, updateData);
        });

        assertEquals("Batch not found", exception.getMessage());
        verify(batchRepository, times(1)).findById(999L);
        verify(batchRepository, never()).save(any());
    }

    /**
     * Kiểm thử luồng điều khiển cho delete()
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh delete()")
    public void testDelete_BranchCoverage() {
        doNothing().when(batchRepository).deleteById(1L);

        batchService.delete(1L);

        verify(batchRepository, times(1)).deleteById(1L);
    }

    // ==========================================
    // 3. PRIVATE METHODS TESTING (via reflection)
    // ==========================================

    /**
     * Test private method applyWarehouseIfPresent() thông qua public create()
     * Branch 1: batch.getWarehouse() == null -> return early
     * Branch 2: batch.getWarehouse() != null -> call setWarehouse()
     */
    @Test
    @DisplayName("Private Method | applyWarehouseIfPresent: Null warehouse case")
    public void testApplyWarehouseIfPresent_NullWarehouse() {
        Batch newBatch = new Batch();
        newBatch.setLotNumber("NOWAREHOUSE");
        newBatch.setWarehouse(null);

        when(medicineRepository.findById(1L)).thenReturn(Optional.of(medicineMock));
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Batch result = batchService.create(1L, newBatch);

        assertNotNull(result);
        assertNull(result.getWarehouse());
        verify(warehouseRepository, never()).findById(any());
    }

    /**
     * Test private method setWarehouse() thông qua public create()
     * Branch 1: warehouse.getWarehouseId() == null -> throw exception
     * Branch 2: warehouse.getWarehouseId() != null -> find and set warehouse
     * Branch 3: warehouseRepository.findById() empty -> throw exception
     */
    @Test
    @DisplayName("Private Method | setWarehouse: Warehouse with null ID")
    public void testSetWarehouse_NullWarehouseId() {
        Warehouse warehouseWithNullId = new Warehouse();
        warehouseWithNullId.setName("Test");
        warehouseWithNullId.setWarehouseId(null);

        Batch newBatch = new Batch();
        newBatch.setLotNumber("NULLID");
        newBatch.setWarehouse(warehouseWithNullId);

        when(medicineRepository.findById(1L)).thenReturn(Optional.of(medicineMock));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            batchService.create(1L, newBatch);
        });

        assertEquals("Warehouse id is required", exception.getMessage());
        verify(warehouseRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Private Method | setWarehouse: Warehouse not found")
    public void testSetWarehouse_WarehouseNotFound() {
        Warehouse nonExistentWarehouse = new Warehouse();
        nonExistentWarehouse.setWarehouseId(999L);
        nonExistentWarehouse.setName("Non-existent");

        Batch newBatch = new Batch();
        newBatch.setLotNumber("NOWAREHOUSE");
        newBatch.setWarehouse(nonExistentWarehouse);

        when(medicineRepository.findById(1L)).thenReturn(Optional.of(medicineMock));
        when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            batchService.create(1L, newBatch);
        });

        assertEquals("Warehouse not found", exception.getMessage());
        verify(warehouseRepository, times(1)).findById(999L);
    }

    // ==========================================
    // 4. SUPPLEMENTARY COVERAGE TESTS
    // ==========================================

    @Test
    @DisplayName("Supplementary: Test create() with repository exception")
    public void testCreate_RepositoryThrowsException() {
        Batch newBatch = new Batch();
        newBatch.setLotNumber("ERROR");

        when(medicineRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> {
            batchService.create(1L, newBatch);
        });

        verify(medicineRepository, times(1)).findById(1L);
        verify(batchRepository, never()).save(any());
    }

    @Test
    @DisplayName("Supplementary: Test update() with null data")
    public void testUpdate_NullData() {
        when(batchRepository.findById(1L)).thenReturn(Optional.of(batchMock));

        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            batchService.update(1L, null);
        });

        assertNotNull(exception);
        verify(batchRepository, times(1)).findById(1L);
        verify(batchRepository, never()).save(any());
    }

    @Test
    @DisplayName("Supplementary: Test update() with all null fields")
    public void testUpdate_AllNullFields() {
        Batch updateData = new Batch();
        // All fields remain null

        when(batchRepository.findById(1L)).thenReturn(Optional.of(batchMock));
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Batch result = batchService.update(1L, updateData);

        assertNotNull(result);
        // Original data should remain unchanged
        assertEquals("LOT001", result.getLotNumber());
        assertEquals(LocalDate.of(2023, 1, 1), result.getManufactureDate());
        assertEquals(LocalDate.of(2025, 12, 31), result.getExpiryDate());
        assertEquals(100, result.getQuantity());
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(warehouseMock, result.getWarehouse());

        verify(batchRepository, times(1)).findById(1L);
        verify(batchRepository, times(1)).save(any(Batch.class));
    }

    @Test
    @DisplayName("Supplementary: Test update() with warehouse having null ID")
    public void testUpdate_WarehouseWithNullId() {
        Batch updateData = new Batch();
        updateData.setLotNumber("UPDATED");

        Warehouse warehouseWithNullId = new Warehouse();
        warehouseWithNullId.setWarehouseId(null);
        updateData.setWarehouse(warehouseWithNullId);

        when(batchRepository.findById(1L)).thenReturn(Optional.of(batchMock));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            batchService.update(1L, updateData);
        });

        assertEquals("Warehouse id is required", exception.getMessage());
        verify(batchRepository, times(1)).findById(1L);
        verify(batchRepository, never()).save(any());
    }

    @Test
    @DisplayName("Supplementary: Test delete() with repository exception")
    public void testDelete_RepositoryThrowsException() {
        doThrow(new RuntimeException("Delete failed")).when(batchRepository).deleteById(1L);

        assertThrows(RuntimeException.class, () -> {
            batchService.delete(1L);
        });

        verify(batchRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Supplementary: Test getByMedicine() with repository exception")
    public void testGetByMedicine_RepositoryThrowsException() {
        when(batchRepository.findByMedicine_MedicineId(1L))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> {
            batchService.getByMedicine(1L);
        });

        verify(batchRepository, times(1)).findByMedicine_MedicineId(1L);
    }

    @Test
    @DisplayName("Supplementary: Test getAll() with repository exception")
    public void testGetAll_RepositoryThrowsException() {
        when(batchRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> {
            batchService.getAll();
        });

        verify(batchRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Supplementary: Test create() with null batch")
    public void testCreate_NullBatch() {
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(medicineMock));

        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            batchService.create(1L, null);
        });

        assertNotNull(exception);
        verify(medicineRepository, times(1)).findById(1L);
        verify(batchRepository, never()).save(any());
    }

    @Test
    @DisplayName("Supplementary: Test update() with empty string status")
    public void testUpdate_EmptyStringStatus() {
        Batch updateData = new Batch();
        updateData.setStatus(""); // Empty string

        when(batchRepository.findById(1L)).thenReturn(Optional.of(batchMock));
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Batch result = batchService.update(1L, updateData);

        assertNotNull(result);
        assertEquals("", result.getStatus());
        // Other fields unchanged
        assertEquals("LOT001", result.getLotNumber());

        verify(batchRepository, times(1)).findById(1L);
        verify(batchRepository, times(1)).save(any(Batch.class));
    }

    @Test
    @DisplayName("Supplementary: Test update() with zero quantity")
    public void testUpdate_ZeroQuantity() {
        Batch updateData = new Batch();
        updateData.setQuantity(0); // Zero quantity

        when(batchRepository.findById(1L)).thenReturn(Optional.of(batchMock));
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Batch result = batchService.update(1L, updateData);

        assertNotNull(result);
        assertEquals(0, result.getQuantity());
        // Other fields unchanged
        assertEquals("LOT001", result.getLotNumber());

        verify(batchRepository, times(1)).findById(1L);
        verify(batchRepository, times(1)).save(any(Batch.class));
    }
}
