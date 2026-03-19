package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.dto.SupplierRequest;
import com.pharmacy.warehouse.dto.SupplierResponse;
import com.pharmacy.warehouse.model.Supplier;
import com.pharmacy.warehouse.model.Supplier.SupplierStatus;
import com.pharmacy.warehouse.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private SupplierService supplierService;

    private Supplier testSupplier;
    private SupplierRequest testRequest;
    private LocalDateTime testTime;

    @BeforeEach
    public void setup() {
        testTime = LocalDateTime.of(2023, 1, 1, 12, 0, 0);
        
        testSupplier = new Supplier();
        testSupplier.setSupplierId(1L);
        testSupplier.setSupplierName("Test Supplier");
        testSupplier.setContactPerson("John Doe");
        testSupplier.setPhoneNumber("1234567890");
        testSupplier.setEmail("test@supplier.com");
        testSupplier.setAddress("123 Test St");
        testSupplier.setTaxCode("TAX123");
        testSupplier.setStatus(SupplierStatus.ACTIVE);
        testSupplier.setCreatedAt(testTime);
        testSupplier.setUpdatedAt(testTime);

        testRequest = new SupplierRequest();
        testRequest.setSupplierName("Test Supplier");
        testRequest.setContactPerson("John Doe");
        testRequest.setPhoneNumber("1234567890");
        testRequest.setEmail("test@supplier.com");
        testRequest.setAddress("123 Test St");
        testRequest.setTaxCode("TAX123");
        testRequest.setStatus("ACTIVE");
    }

    // ==========================================
    // 1. BLACK-BOX TESTING (EP, BVA)
    // ==========================================

    /**
     * Kiểm thử Hộp đen: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
     * Target Hàm: getAllSuppliers()
     * Phân tích kết quả trả về từ repository:
     * - Empty list -> trả về empty list
     * - Single item list -> trả về single item list  
     * - Multiple items list -> trả về multiple items list
     */
    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra getAllSuppliers() với các trường hợp khác nhau")
    public void testGetAllSuppliers_EquivalencePartition_BoundaryValue() {
        // Case 1: Empty list - Phân vùng rỗng
        when(supplierRepository.findAll()).thenReturn(new ArrayList<>());
        List<SupplierResponse> result1 = supplierService.getAllSuppliers();
        assertTrue(result1.isEmpty());
        verify(supplierRepository, times(1)).findAll();

        // Case 2: Single item - Giá trị biên dưới
        when(supplierRepository.findAll()).thenReturn(List.of(testSupplier));
        List<SupplierResponse> result2 = supplierService.getAllSuppliers();
        assertEquals(1, result2.size());
        assertEquals("Test Supplier", result2.get(0).getSupplierName());
        verify(supplierRepository, times(2)).findAll();

        // Case 3: Multiple items - Phân vùng bình thường
        Supplier supplier2 = new Supplier();
        supplier2.setSupplierId(2L);
        supplier2.setSupplierName("Second Supplier");
        supplier2.setContactPerson("Jane Smith");
        supplier2.setStatus(SupplierStatus.INACTIVE);
        supplier2.setCreatedAt(testTime);
        supplier2.setUpdatedAt(testTime);
        
        when(supplierRepository.findAll()).thenReturn(List.of(testSupplier, supplier2));
        List<SupplierResponse> result3 = supplierService.getAllSuppliers();
        assertEquals(2, result3.size());
        assertEquals("Test Supplier", result3.get(0).getSupplierName());
        assertEquals("Second Supplier", result3.get(1).getSupplierName());
        verify(supplierRepository, times(3)).findAll();
    }

    /**
     * Kiểm thử Hộp đen: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
     * Target Hàm: getActiveSuppliers()
     * Phân tích kết quả trả về từ repository:
     * - No active suppliers -> empty list
     * - Some active suppliers -> list of active suppliers
     * - All suppliers active -> all suppliers returned
     */
    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra getActiveSuppliers() với các trường hợp khác nhau")
    public void testGetActiveSuppliers_EquivalencePartition_BoundaryValue() {
        // Case 1: No active suppliers
        when(supplierRepository.findAllActiveSuppliers()).thenReturn(new ArrayList<>());
        List<SupplierResponse> result1 = supplierService.getActiveSuppliers();
        assertTrue(result1.isEmpty());
        verify(supplierRepository, times(1)).findAllActiveSuppliers();

        // Case 2: Some active suppliers
        when(supplierRepository.findAllActiveSuppliers()).thenReturn(List.of(testSupplier));
        List<SupplierResponse> result2 = supplierService.getActiveSuppliers();
        assertEquals(1, result2.size());
        assertEquals("Test Supplier", result2.get(0).getSupplierName());
        assertEquals("ACTIVE", result2.get(0).getStatus());
        verify(supplierRepository, times(2)).findAllActiveSuppliers();

        // Case 3: Multiple active suppliers
        Supplier supplier2 = new Supplier();
        supplier2.setSupplierId(2L);
        supplier2.setSupplierName("Second Active Supplier");
        supplier2.setStatus(SupplierStatus.ACTIVE);
        supplier2.setCreatedAt(testTime);
        supplier2.setUpdatedAt(testTime);
        
        when(supplierRepository.findAllActiveSuppliers()).thenReturn(List.of(testSupplier, supplier2));
        List<SupplierResponse> result3 = supplierService.getActiveSuppliers();
        assertEquals(2, result3.size());
        verify(supplierRepository, times(3)).findAllActiveSuppliers();
    }

    // ==========================================
    // 2. WHITE-BOX TESTING (CFG, DFG)
    // ==========================================

    /**
     * Kiểm thử luồng điều khiển (CFG - Branch Coverage)
     * Dành cho hàm getSupplierById(Long id):
     * - Branch 1: supplierRepository.findById() trả về Optional.isPresent() -> tiếp tục xử lý
     * - Branch 2: supplierRepository.findById() trả về Optional.isEmpty() -> ném RuntimeException
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getSupplierById() - Success path")
    public void testGetSupplierById_SuccessPath_BranchCoverage() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));

        SupplierResponse result = supplierService.getSupplierById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getSupplierId());
        assertEquals("Test Supplier", result.getSupplierName());
        assertEquals("John Doe", result.getContactPerson());
        assertEquals("1234567890", result.getPhoneNumber());
        assertEquals("test@supplier.com", result.getEmail());
        assertEquals("123 Test St", result.getAddress());
        assertEquals("TAX123", result.getTaxCode());
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(testTime, result.getCreatedAt());
        assertEquals(testTime, result.getUpdatedAt());

        verify(supplierRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getSupplierById() - Exception path")
    public void testGetSupplierById_NotFound_BranchCoverage() {
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            supplierService.getSupplierById(999L);
        });

        assertEquals("Supplier not found with id: 999", exception.getMessage());
        verify(supplierRepository, times(1)).findById(999L);
    }

    /**
     * Phân tích luồng dữ liệu (DFG - Data Flow Graph)
     * Hàm: createSupplier(SupplierRequest request)
     * Quy trình DFG:
     * 1. Define parameter `request` -> Use trong validation checks
     * 2. Use request.getTaxCode() -> repository.findByTaxCode() -> validation
     * 3. Use request fields -> set vào supplier object
     * 4. Use supplier -> repository.save() -> Define savedSupplier
     * 5. Use savedSupplier -> convertToResponse() -> return result
     */
    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng dữ liệu create() đầy đủ")
    public void testCreateSupplier_CompleteDataFlow() {
        when(supplierRepository.findByTaxCode("TAX123")).thenReturn(Optional.empty());
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier saved = invocation.getArgument(0);
            saved.setSupplierId(1L);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });

        SupplierResponse result = supplierService.createSupplier(testRequest);

        assertNotNull(result);
        assertEquals(1L, result.getSupplierId());
        assertEquals("Test Supplier", result.getSupplierName());
        assertEquals("John Doe", result.getContactPerson());
        assertEquals("1234567890", result.getPhoneNumber());
        assertEquals("test@supplier.com", result.getEmail());
        assertEquals("123 Test St", result.getAddress());
        assertEquals("TAX123", result.getTaxCode());
        assertEquals("ACTIVE", result.getStatus());

        verify(supplierRepository, times(1)).findByTaxCode("TAX123");
        verify(supplierRepository, times(1)).save(any(Supplier.class));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createSupplier() - Tax code conflict")
    public void testCreateSupplier_TaxCodeConflict_BranchCoverage() {
        Supplier existingSupplier = new Supplier();
        existingSupplier.setSupplierId(2L);
        existingSupplier.setTaxCode("TAX123");

        when(supplierRepository.findByTaxCode("TAX123")).thenReturn(Optional.of(existingSupplier));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            supplierService.createSupplier(testRequest);
        });

        assertEquals("Supplier with tax code TAX123 already exists", exception.getMessage());
        verify(supplierRepository, times(1)).findByTaxCode("TAX123");
        verify(supplierRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createSupplier() - Null tax code")
    public void testCreateSupplier_NullTaxCode_BranchCoverage() {
        testRequest.setTaxCode(null);

        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier saved = invocation.getArgument(0);
            saved.setSupplierId(1L);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });

        SupplierResponse result = supplierService.createSupplier(testRequest);

        assertNotNull(result);
        assertNull(result.getTaxCode());

        verify(supplierRepository, never()).findByTaxCode(any());
        verify(supplierRepository, times(1)).save(any(Supplier.class));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createSupplier() - Null status (default to ACTIVE)")
    public void testCreateSupplier_NullStatus_BranchCoverage() {
        testRequest.setStatus(null);

        when(supplierRepository.findByTaxCode("TAX123")).thenReturn(Optional.empty());
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier saved = invocation.getArgument(0);
            saved.setSupplierId(1L);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });

        SupplierResponse result = supplierService.createSupplier(testRequest);

        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus()); // Default status

        verify(supplierRepository, times(1)).findByTaxCode("TAX123");
        verify(supplierRepository, times(1)).save(any(Supplier.class));
    }

    /**
     * Phân tích luồng dữ liệu (DFG - Data Flow Graph) phức tạp
     * Hàm: updateSupplier(Long id, SupplierRequest request)
     * Quy trình DFG:
     * 1. Define `id` -> Use trong findById() -> Define `supplier`
     * 2. Use `request` fields -> Use trong setter methods
     * 3. Use `supplier` -> Use trong save() -> Define `updatedSupplier`
     * 4. Use `updatedSupplier` -> convertToResponse() -> return result
     */
    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng dữ liệu update() phức tạp")
    public void testUpdateSupplier_ComplexDataFlow() {
        SupplierRequest updateRequest = new SupplierRequest();
        updateRequest.setSupplierName("Updated Supplier");
        updateRequest.setContactPerson("Updated Contact");
        updateRequest.setPhoneNumber("9876543210");
        updateRequest.setEmail("updated@supplier.com");
        updateRequest.setAddress("456 Updated St");
        updateRequest.setTaxCode("UPDATED123");
        updateRequest.setStatus("INACTIVE");

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupplierResponse result = supplierService.updateSupplier(1L, updateRequest);

        assertNotNull(result);
        assertEquals("Updated Supplier", result.getSupplierName());
        assertEquals("Updated Contact", result.getContactPerson());
        assertEquals("9876543210", result.getPhoneNumber());
        assertEquals("updated@supplier.com", result.getEmail());
        assertEquals("456 Updated St", result.getAddress());
        assertEquals("UPDATED123", result.getTaxCode());
        assertEquals("INACTIVE", result.getStatus());

        verify(supplierRepository, times(1)).findById(1L);
        verify(supplierRepository, times(1)).save(any(Supplier.class));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh updateSupplier() - Supplier not found")
    public void testUpdateSupplier_NotFound_BranchCoverage() {
        SupplierRequest updateRequest = new SupplierRequest();
        updateRequest.setSupplierName("Updated Supplier");

        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            supplierService.updateSupplier(999L, updateRequest);
        });

        assertEquals("Supplier not found with id: 999", exception.getMessage());
        verify(supplierRepository, times(1)).findById(999L);
        verify(supplierRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh updateSupplier() - Null status (no status update)")
    public void testUpdateSupplier_NullStatus_BranchCoverage() {
        SupplierRequest updateRequest = new SupplierRequest();
        updateRequest.setSupplierName("Updated Supplier");
        updateRequest.setStatus(null); // Null status

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupplierResponse result = supplierService.updateSupplier(1L, updateRequest);

        assertNotNull(result);
        assertEquals("Updated Supplier", result.getSupplierName());
        assertEquals("ACTIVE", result.getStatus()); // Original status unchanged

        verify(supplierRepository, times(1)).findById(1L);
        verify(supplierRepository, times(1)).save(any(Supplier.class));
    }

    /**
     * Kiểm thử luồng điều khiển cho deleteSupplier()
     * Soft delete by setting status to INACTIVE
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh deleteSupplier() - Soft delete")
    public void testDeleteSupplier_SoftDelete_BranchCoverage() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        supplierService.deleteSupplier(1L);

        // Verify status was changed to INACTIVE
        assertEquals(SupplierStatus.INACTIVE, testSupplier.getStatus());

        verify(supplierRepository, times(1)).findById(1L);
        verify(supplierRepository, times(1)).save(testSupplier);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh deleteSupplier() - Supplier not found")
    public void testDeleteSupplier_NotFound_BranchCoverage() {
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            supplierService.deleteSupplier(999L);
        });

        assertEquals("Supplier not found with id: 999", exception.getMessage());
        verify(supplierRepository, times(1)).findById(999L);
        verify(supplierRepository, never()).save(any());
    }

    // ==========================================
    // 3. PRIVATE METHOD TESTING
    // ==========================================

    /**
     * Test private method convertToResponse() thông qua các public methods
     * Verify tất cả các field được map đúng cách
     */
    @Test
    @DisplayName("Private Method | convertToResponse: Test complete field mapping")
    public void testConvertToResponse_CompleteFieldMapping() {
        // Test with all fields populated
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));

        SupplierResponse result = supplierService.getSupplierById(1L);

        assertNotNull(result);
        assertEquals(testSupplier.getSupplierId(), result.getSupplierId());
        assertEquals(testSupplier.getSupplierName(), result.getSupplierName());
        assertEquals(testSupplier.getContactPerson(), result.getContactPerson());
        assertEquals(testSupplier.getPhoneNumber(), result.getPhoneNumber());
        assertEquals(testSupplier.getEmail(), result.getEmail());
        assertEquals(testSupplier.getAddress(), result.getAddress());
        assertEquals(testSupplier.getTaxCode(), result.getTaxCode());
        assertEquals(testSupplier.getStatus().name(), result.getStatus());
        assertEquals(testSupplier.getCreatedAt(), result.getCreatedAt());
        assertEquals(testSupplier.getUpdatedAt(), result.getUpdatedAt());

        verify(supplierRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Private Method | convertToResponse: Test with null fields")
    public void testConvertToResponse_NullFields() {
        // Test with null fields
        Supplier supplierWithNulls = new Supplier();
        supplierWithNulls.setSupplierId(1L);
        supplierWithNulls.setSupplierName("Test");
        supplierWithNulls.setStatus(SupplierStatus.ACTIVE);
        supplierWithNulls.setCreatedAt(testTime);
        supplierWithNulls.setUpdatedAt(testTime);
        // Other fields remain null

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplierWithNulls));

        SupplierResponse result = supplierService.getSupplierById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getSupplierId());
        assertEquals("Test", result.getSupplierName());
        assertNull(result.getContactPerson());
        assertNull(result.getPhoneNumber());
        assertNull(result.getEmail());
        assertNull(result.getAddress());
        assertNull(result.getTaxCode());
        assertEquals("ACTIVE", result.getStatus());

        verify(supplierRepository, times(1)).findById(1L);
    }

    // ==========================================
    // 4. SUPPLEMENTARY COVERAGE TESTS
    // ==========================================

    @Test
    @DisplayName("Supplementary: Test createSupplier() with different statuses")
    public void testCreateSupplier_DifferentStatuses() {
        // Test with SUSPENDED status
        testRequest.setStatus("SUSPENDED");

        when(supplierRepository.findByTaxCode("TAX123")).thenReturn(Optional.empty());
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier saved = invocation.getArgument(0);
            saved.setSupplierId(1L);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });

        SupplierResponse result = supplierService.createSupplier(testRequest);

        assertNotNull(result);
        assertEquals("SUSPENDED", result.getStatus());

        verify(supplierRepository, times(1)).findByTaxCode("TAX123");
        verify(supplierRepository, times(1)).save(any(Supplier.class));
    }

    @Test
    @DisplayName("Supplementary: Test createSupplier() with empty tax code")
    public void testCreateSupplier_EmptyTaxCode() {
        testRequest.setTaxCode("");

        when(supplierRepository.findByTaxCode("")).thenReturn(Optional.empty());
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier saved = invocation.getArgument(0);
            saved.setSupplierId(1L);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });

        SupplierResponse result = supplierService.createSupplier(testRequest);

        assertNotNull(result);
        assertEquals("", result.getTaxCode());

        verify(supplierRepository, times(1)).findByTaxCode("");
        verify(supplierRepository, times(1)).save(any(Supplier.class));
    }

    @Test
    @DisplayName("Supplementary: Test updateSupplier() with partial data")
    public void testUpdateSupplier_PartialData() {
        SupplierRequest partialUpdate = new SupplierRequest();
        partialUpdate.setSupplierName("Partial Update");
        // Only update name, other fields null

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupplierResponse result = supplierService.updateSupplier(1L, partialUpdate);

        assertNotNull(result);
        assertEquals("Partial Update", result.getSupplierName());
        // Other fields should be null since they were set to null in request
        assertNull(result.getContactPerson());
        assertNull(result.getPhoneNumber());
        assertNull(result.getEmail());
        assertNull(result.getAddress());
        assertNull(result.getTaxCode());
        assertEquals("ACTIVE", result.getStatus()); // Status unchanged

        verify(supplierRepository, times(1)).findById(1L);
        verify(supplierRepository, times(1)).save(any(Supplier.class));
    }

    @Test
    @DisplayName("Supplementary: Test getAllSuppliers repository exception")
    public void testGetAllSuppliersRepositoryException() {
        when(supplierRepository.findAll()).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            supplierService.getAllSuppliers();
        });
    }

    @Test
    @DisplayName("Supplementary: Test getActiveSuppliers repository exception")
    public void testGetActiveSuppliersRepositoryException() {
        when(supplierRepository.findAllActiveSuppliers()).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            supplierService.getActiveSuppliers();
        });
    }

    @Test
    @DisplayName("Supplementary: Test getSupplierById repository exception")
    public void testGetSupplierByIdRepositoryException() {
        when(supplierRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            supplierService.getSupplierById(1L);
        });
    }

    @Test
    @DisplayName("Supplementary: Test createSupplier repository exception")
    public void testCreateSupplierRepositoryException() {
        when(supplierRepository.findByTaxCode("TAX123")).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            supplierService.createSupplier(testRequest);
        });
    }

    @Test
    @DisplayName("Supplementary: Test updateSupplier repository exception")
    public void testUpdateSupplierRepositoryException() {
        when(supplierRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            supplierService.updateSupplier(1L, testRequest);
        });
    }

    @Test
    @DisplayName("Supplementary: Test deleteSupplier repository exception")
    public void testDeleteSupplierRepositoryException() {
        when(supplierRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            supplierService.deleteSupplier(1L);
        });
    }

    @Test
    @DisplayName("Supplementary: Test createSupplier() with save exception")
    public void testCreateSupplier_SaveException() {
        when(supplierRepository.findByTaxCode("TAX123")).thenReturn(Optional.empty());
        when(supplierRepository.save(any(Supplier.class))).thenThrow(new RuntimeException("Save failed"));

        assertThrows(RuntimeException.class, () -> {
            supplierService.createSupplier(testRequest);
        });

        verify(supplierRepository, times(1)).findByTaxCode("TAX123");
        verify(supplierRepository, times(1)).save(any(Supplier.class));
    }

    @Test
    @DisplayName("Supplementary: Test updateSupplier() with save exception")
    public void testUpdateSupplier_SaveException() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenThrow(new RuntimeException("Save failed"));

        assertThrows(RuntimeException.class, () -> {
            supplierService.updateSupplier(1L, testRequest);
        });

        verify(supplierRepository, times(1)).findById(1L);
        verify(supplierRepository, times(1)).save(any(Supplier.class));
    }

    @Test
    @DisplayName("Supplementary: Test deleteSupplier() with save exception")
    public void testDeleteSupplier_SaveException() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenThrow(new RuntimeException("Save failed"));

        assertThrows(RuntimeException.class, () -> {
            supplierService.deleteSupplier(1L);
        });

        verify(supplierRepository, times(1)).findById(1L);
        verify(supplierRepository, times(1)).save(any(Supplier.class));
    }

    @Test
    @DisplayName("Supplementary: Test createSupplier() with invalid status enum")
    public void testCreateSupplier_InvalidStatusEnum() {
        testRequest.setStatus("INVALID_STATUS");

        when(supplierRepository.findByTaxCode("TAX123")).thenReturn(Optional.empty());

        // This should throw IllegalArgumentException from valueOf()
        assertThrows(IllegalArgumentException.class, () -> {
            supplierService.createSupplier(testRequest);
        });

        verify(supplierRepository, times(1)).findByTaxCode("TAX123");
    }

    @Test
    @DisplayName("Supplementary: Test updateSupplier() with invalid status enum")
    public void testUpdateSupplier_InvalidStatusEnum() {
        SupplierRequest invalidRequest = new SupplierRequest();
        invalidRequest.setSupplierName("Test");
        invalidRequest.setStatus("INVALID_STATUS");

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));

        // This should throw IllegalArgumentException from valueOf()
        assertThrows(IllegalArgumentException.class, () -> {
            supplierService.updateSupplier(1L, invalidRequest);
        });

        verify(supplierRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Supplementary: Test deleteSupplier() already inactive")
    public void testDeleteSupplier_AlreadyInactive() {
        testSupplier.setStatus(SupplierStatus.INACTIVE);

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        supplierService.deleteSupplier(1L);

        // Should still save, but status remains INACTIVE
        assertEquals(SupplierStatus.INACTIVE, testSupplier.getStatus());

        verify(supplierRepository, times(1)).findById(1L);
        verify(supplierRepository, times(1)).save(testSupplier);
    }
}
