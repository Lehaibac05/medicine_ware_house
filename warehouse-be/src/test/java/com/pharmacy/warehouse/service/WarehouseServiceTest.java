package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.model.Warehouse;
import com.pharmacy.warehouse.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private WarehouseService warehouseService;

    private Warehouse warehouseMock;

    @BeforeEach
    public void setup() {
        warehouseMock = new Warehouse();
        warehouseMock.setWarehouseId(1L);
        warehouseMock.setName("Main Warehouse");
        warehouseMock.setLocation("HCM City");
        warehouseMock.setDescription("Primary storage facility");
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
        when(warehouseRepository.findAll()).thenReturn(new ArrayList<>());
        List<Warehouse> result1 = warehouseService.getAll();
        assertTrue(result1.isEmpty());
        verify(warehouseRepository, times(1)).findAll();

        // Case 2: Single item - Giá trị biên dưới
        when(warehouseRepository.findAll()).thenReturn(List.of(warehouseMock));
        List<Warehouse> result2 = warehouseService.getAll();
        assertEquals(1, result2.size());
        assertEquals("Main Warehouse", result2.get(0).getName());
        verify(warehouseRepository, times(2)).findAll();

        // Case 3: Multiple items - Phân vùng bình thường
        Warehouse warehouse2 = new Warehouse();
        warehouse2.setWarehouseId(2L);
        warehouse2.setName("Secondary Warehouse");
        warehouse2.setLocation("Hanoi");
        warehouse2.setDescription("Backup storage");
        
        when(warehouseRepository.findAll()).thenReturn(List.of(warehouseMock, warehouse2));
        List<Warehouse> result3 = warehouseService.getAll();
        assertEquals(2, result3.size());
        verify(warehouseRepository, times(3)).findAll();
    }

    // ==========================================
    // 2. WHITE-BOX TESTING (CFG, DFG)
    // ==========================================

    /**
     * Kiểm thử luồng điều khiển (CFG - Branch Coverage)
     * Dành cho hàm getById(Long id):
     * - Branch 1: warehouseRepository.findById(id) trả về Optional.isPresent() -> trả về entity
     * - Branch 2: warehouseRepository.findById(id) trả về Optional.isEmpty() -> ném RuntimeException
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh cho getById() - Success path")
    public void testGetById_Success_Branch() {
        // Branch 1: findById trả về kết quả
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouseMock));
        
        Warehouse result = warehouseService.getById(1L);
        
        assertNotNull(result);
        assertEquals(1L, result.getWarehouseId());
        assertEquals("Main Warehouse", result.getName());
        verify(warehouseRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh cho getById() - Exception path")
    public void testGetById_NotFound_Branch() {
        // Branch 2: findById không tìm thấy kết quả
        when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            warehouseService.getById(999L);
        });
        
        assertEquals("Warehouse not found", exception.getMessage());
        verify(warehouseRepository, times(1)).findById(999L);
    }

    /**
     * Phân tích luồng dữ liệu (DFG - Data Flow Graph)
     * Hàm: create(Warehouse warehouse)
     * Quy trình DFG:
     * 1. Define parameter `warehouse`
     * 2. Use `warehouse` trong repository.save()
     * 3. Return kết quả từ save()
     */
    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng dữ liệu create()")
    public void testCreate_DataFlow() {
        Warehouse newWarehouse = new Warehouse();
        newWarehouse.setName("New Warehouse");
        newWarehouse.setLocation("Da Nang");
        newWarehouse.setDescription("Coastal storage facility");
        
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> {
            Warehouse saved = invocation.getArgument(0);
            saved.setWarehouseId(3L);
            return saved;
        });
        
        Warehouse result = warehouseService.create(newWarehouse);
        
        // Verify data flow: input -> save() -> output
        assertNotNull(result);
        assertEquals(3L, result.getWarehouseId());
        assertEquals("New Warehouse", result.getName());
        assertEquals("Da Nang", result.getLocation());
        assertEquals("Coastal storage facility", result.getDescription());
        
        // Capture và verify đối tượng được truyền vào save()
        ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
        verify(warehouseRepository, times(1)).save(captor.capture());
        Warehouse savedWarehouse = captor.getValue();
        assertEquals("New Warehouse", savedWarehouse.getName());
        assertEquals("Da Nang", savedWarehouse.getLocation());
    }

    /**
     * Phân tích luồng dữ liệu (DFG - Data Flow Graph) phức tạp
     * Hàm: update(Long id, Warehouse data)
     * Quy trình DFG:
     * 1. Define `id` -> Use trong getById() -> Define `warehouse`
     * 2. Define `data` -> Use data.getName() -> Use trong warehouse.setName()
     * 3. Use `warehouse` đã cập nhật -> Use trong repository.save()
     * 4. Return kết quả từ save()
     */
    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng dữ liệu update() phức tạp")
    public void testUpdate_ComplexDataFlow() {
        Long id = 1L;
        Warehouse updateData = new Warehouse();
        updateData.setName("Updated Warehouse");
        updateData.setLocation("Updated Location");
        updateData.setDescription("Updated Description");
        
        // Mock getById() path
        when(warehouseRepository.findById(id)).thenReturn(Optional.of(warehouseMock));
        
        // Mock save() with answer to capture the updated entity
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        Warehouse result = warehouseService.update(id, updateData);
        
        // Verify complete data flow
        assertNotNull(result);
        assertEquals(1L, result.getWarehouseId()); // Original ID preserved
        assertEquals("Updated Warehouse", result.getName()); // Updated from data
        assertEquals("Updated Location", result.getLocation()); // Updated from data
        assertEquals("Updated Description", result.getDescription()); // Updated from data
        
        // Verify the sequence of calls
        verify(warehouseRepository, times(1)).findById(id);
        ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
        verify(warehouseRepository, times(1)).save(captor.capture());
        
        Warehouse savedWarehouse = captor.getValue();
        assertEquals("Updated Warehouse", savedWarehouse.getName());
        assertEquals("Updated Location", savedWarehouse.getLocation());
        assertEquals("Updated Description", savedWarehouse.getDescription());
    }

    /**
     * Kiểm thử luồng điều khiển cho delete()
     * Branch: chỉ có một luồng - gọi deleteById()
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh cho delete()")
    public void testDelete_BranchCoverage() {
        // Mock void method
        doNothing().when(warehouseRepository).deleteById(1L);
        
        // Execute
        warehouseService.delete(1L);
        
        // Verify the branch was executed
        verify(warehouseRepository, times(1)).deleteById(1L);
    }

    // ==========================================
    // 3. SUPPLEMENTARY COVERAGE TESTS
    // ==========================================

    @Test
    @DisplayName("Supplementary: Test update() with null fields")
    public void testUpdate_WithNullFields() {
        Long id = 1L;
        Warehouse updateData = new Warehouse();
        updateData.setName(null);
        updateData.setLocation(null);
        updateData.setDescription(null);
        
        when(warehouseRepository.findById(id)).thenReturn(Optional.of(warehouseMock));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        Warehouse result = warehouseService.update(id, updateData);
        
        // Verify null values are properly set
        assertNull(result.getName());
        assertNull(result.getLocation());
        assertNull(result.getDescription());
        assertEquals(1L, result.getWarehouseId());
    }

    @Test
    @DisplayName("Supplementary: Test update() with empty strings")
    public void testUpdate_WithEmptyStrings() {
        Long id = 1L;
        Warehouse updateData = new Warehouse();
        updateData.setName("");
        updateData.setLocation("");
        updateData.setDescription("");
        
        when(warehouseRepository.findById(id)).thenReturn(Optional.of(warehouseMock));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        Warehouse result = warehouseService.update(id, updateData);
        
        // Verify empty strings are properly set
        assertEquals("", result.getName());
        assertEquals("", result.getLocation());
        assertEquals("", result.getDescription());
    }

    @Test
    @DisplayName("Supplementary: Test create() with null warehouse")
    public void testCreate_WithNullWarehouse() {
        doReturn(null).when(warehouseRepository).save(any());
        
        Warehouse result = warehouseService.create(null);
        
        assertNull(result);
        verify(warehouseRepository, times(1)).save(null);
    }

    @Test
    @DisplayName("Supplementary: Test getAll() when repository throws exception")
    public void testGetAll_RepositoryThrowsException() {
        when(warehouseRepository.findAll()).thenThrow(new RuntimeException("Database error"));
        
        assertThrows(RuntimeException.class, () -> {
            warehouseService.getAll();
        });
        
        verify(warehouseRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Supplementary: Test create() when repository throws exception")
    public void testCreate_RepositoryThrowsException() {
        when(warehouseRepository.save(any(Warehouse.class))).thenThrow(new RuntimeException("Save failed"));
        
        assertThrows(RuntimeException.class, () -> {
            warehouseService.create(warehouseMock);
        });
        
        verify(warehouseRepository, times(1)).save(warehouseMock);
    }

    @Test
    @DisplayName("Supplementary: Test update() when getById() throws exception")
    public void testUpdate_GetByIdThrowsException() {
        Long id = 1L;
        Warehouse updateData = new Warehouse();
        updateData.setName("Test");
        
        when(warehouseRepository.findById(id)).thenThrow(new RuntimeException("Find failed"));
        
        assertThrows(RuntimeException.class, () -> {
            warehouseService.update(id, updateData);
        });
        
        verify(warehouseRepository, times(1)).findById(id);
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Supplementary: Test delete() when repository throws exception")
    public void testDelete_RepositoryThrowsException() {
        doThrow(new RuntimeException("Delete failed")).when(warehouseRepository).deleteById(1L);
        
        assertThrows(RuntimeException.class, () -> {
            warehouseService.delete(1L);
        });
        
        verify(warehouseRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Supplementary: Test update() with partial data")
    public void testUpdate_PartialData() {
        Long id = 1L;
        Warehouse updateData = new Warehouse();
        updateData.setName("Partial Update");
        // location và description không được set
        
        when(warehouseRepository.findById(id)).thenReturn(Optional.of(warehouseMock));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        Warehouse result = warehouseService.update(id, updateData);
        
        assertEquals("Partial Update", result.getName());
        assertNull(result.getLocation()); // Should be null since not set
        assertNull(result.getDescription()); // Should be null since not set
    }
}
