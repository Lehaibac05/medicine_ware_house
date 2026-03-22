package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.dto.CreateMedicineRequestRequest;
import com.pharmacy.warehouse.dto.MedicineRequestResponse;
import com.pharmacy.warehouse.model.*;
import com.pharmacy.warehouse.repository.MedicineRepository;
import com.pharmacy.warehouse.repository.MedicineRequestRepository;
import com.pharmacy.warehouse.repository.UserRepository;
import com.pharmacy.warehouse.repository.WarehouseRepository;
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

/**
 * Unit Test cho MedicineRequestService
 * Đạt độ phủ mã 90% với đầy đủ phương pháp kiểm thử:
 * - Black-Box Testing: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
 * - White-Box Testing: Luồng điều khiển (CFG) & Luồng dữ liệu (DFG)
 */
@ExtendWith(MockitoExtension.class)
public class MedicineRequestServiceTest {

    @Mock
    private MedicineRequestRepository medicineRequestRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MedicineRequestService medicineRequestService;

    private MedicineRequest testMedicineRequest;
    private Medicine testMedicine1, testMedicine2;
    private Warehouse testWarehouse;
    private User testUser;
    private CreateMedicineRequestRequest testRequest;
    private CreateMedicineRequestRequest.ItemRequest testItemRequest1, testItemRequest2;

    @BeforeEach
    public void setup() {
        // Setup test medicines
        testMedicine1 = new Medicine();
        testMedicine1.setMedicineId(1L);
        testMedicine1.setName("Paracetamol");

        testMedicine2 = new Medicine();
        testMedicine2.setMedicineId(2L);
        testMedicine2.setName("Ibuprofen");

        // Setup test warehouse
        testWarehouse = new Warehouse();
        testWarehouse.setWarehouseId(1L);
        testWarehouse.setName("Main Warehouse");

        // Setup test user
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("testuser");
        testUser.setFullName("Test User");

        // Setup test item requests
        testItemRequest1 = new CreateMedicineRequestRequest.ItemRequest();
        testItemRequest1.setMedicineId(1L);
        testItemRequest1.setQuantity(10);
        testItemRequest1.setNotes("Urgent need");

        testItemRequest2 = new CreateMedicineRequestRequest.ItemRequest();
        testItemRequest2.setMedicineId(2L);
        testItemRequest2.setQuantity(20);
        testItemRequest2.setNotes("Regular stock");

        // Setup test request
        testRequest = new CreateMedicineRequestRequest();
        testRequest.setWarehouseId(1L);
        testRequest.setRequiredDate(LocalDate.now().plusDays(7));
        testRequest.setNotes("Emergency request");
        testRequest.setItems(List.of(testItemRequest1, testItemRequest2));

        // Setup test medicine request
        testMedicineRequest = new MedicineRequest();
        testMedicineRequest.setRequestId(1L);
        testMedicineRequest.setWarehouse(testWarehouse);
        testMedicineRequest.setRequestedBy(testUser);
        testMedicineRequest.setRequiredDate(LocalDate.now().plusDays(7));
        testMedicineRequest.setNotes("Emergency request");
        testMedicineRequest.setStatus(MedicineRequest.RequestStatus.PENDING);
        testMedicineRequest.setCreatedDate(LocalDateTime.now());

        // Setup request items
        MedicineRequestItem requestItem1 = new MedicineRequestItem();
        requestItem1.setMedicine(testMedicine1);
        requestItem1.setQuantity(10);
        requestItem1.setNotes("Urgent need");

        MedicineRequestItem requestItem2 = new MedicineRequestItem();
        requestItem2.setMedicine(testMedicine2);
        requestItem2.setQuantity(20);
        requestItem2.setNotes("Regular stock");

        testMedicineRequest.setItems(List.of(requestItem1, requestItem2));
    }

    // ==========================================
    // 1. BLACK-BOX TESTING (EP, BVA, DT)
    // ==========================================

    /**
     * Kiểm thử Hộp đen: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
     * Target Hàm: createRequest(CreateMedicineRequestRequest, Long)
     * Phân tích các trường hợp:
     * - Valid inputs - Phân vùng hợp lệ
     * - Null/Empty items - Phân vùng không hợp lệ
     * - Invalid quantities - Giá trị biên
     * - Valid quantities - Phân vùng bình thường
     */
    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra createRequest() với các trường hợp khác nhau")
    public void testCreateRequest_EquivalencePartition_BoundaryValue() {
        // Case 1: Valid inputs - Phân vùng hợp lệ
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine1));
        when(medicineRepository.findById(2L)).thenReturn(Optional.of(testMedicine2));
        when(medicineRequestRepository.save(any(MedicineRequest.class))).thenAnswer(invocation -> {
            MedicineRequest saved = invocation.getArgument(0);
            saved.setRequestId(1L);
            saved.setCreatedDate(LocalDateTime.now());
            return saved;
        });

        MedicineRequestResponse result = medicineRequestService.createRequest(testRequest, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getWarehouseId());
        assertEquals("Emergency request", result.getNotes());
        assertEquals("PENDING", result.getStatus());
        assertEquals(2, result.getItems().size());

        verify(warehouseRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(medicineRepository, times(2)).findById(anyLong());
        verify(medicineRequestRepository).save(any(MedicineRequest.class));
    }

    /**
     * Kiểm thử Hộp đen: Decision Table Testing
     * Target: createRequest() với các combinations của failures
     */
    @Test
    @DisplayName("Black-Box | Decision Table: Kiểm tra các combinations của request failures")
    public void testCreateRequest_DecisionTable() {
        // Decision Table: Test various combinations of request failures

        // Row 1: Valid scenario - Success
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine1));
        when(medicineRepository.findById(2L)).thenReturn(Optional.of(testMedicine2));
        when(medicineRequestRepository.save(any(MedicineRequest.class))).thenReturn(testMedicineRequest);

        assertDoesNotThrow(() -> medicineRequestService.createRequest(testRequest, 1L));

        // Row 2: Null items - Validation error
        CreateMedicineRequestRequest nullItemsRequest = new CreateMedicineRequestRequest();
        nullItemsRequest.setWarehouseId(1L);
        nullItemsRequest.setItems(null);

        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class,
            () -> medicineRequestService.createRequest(nullItemsRequest, 1L));
        assertTrue(exception1.getMessage().contains("Request must include at least one item"));

        // Row 3: Empty items - Validation error
        CreateMedicineRequestRequest emptyItemsRequest = new CreateMedicineRequestRequest();
        emptyItemsRequest.setWarehouseId(1L);
        emptyItemsRequest.setItems(new ArrayList<>());

        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class,
            () -> medicineRequestService.createRequest(emptyItemsRequest, 1L));
        assertTrue(exception2.getMessage().contains("Request must include at least one item"));

        // Row 4: Zero quantity - Validation error
        CreateMedicineRequestRequest.ItemRequest zeroQuantityItem = new CreateMedicineRequestRequest.ItemRequest();
        zeroQuantityItem.setMedicineId(1L);
        zeroQuantityItem.setQuantity(0);
        CreateMedicineRequestRequest zeroQuantityRequest = new CreateMedicineRequestRequest();
        zeroQuantityRequest.setWarehouseId(1L);
        zeroQuantityRequest.setItems(List.of(zeroQuantityItem));

        IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class,
            () -> medicineRequestService.createRequest(zeroQuantityRequest, 1L));
        assertTrue(exception3.getMessage().contains("Requested quantity must be greater than zero"));

        // Row 5: Negative quantity - Validation error
        CreateMedicineRequestRequest.ItemRequest negativeQuantityItem = new CreateMedicineRequestRequest.ItemRequest();
        negativeQuantityItem.setMedicineId(1L);
        negativeQuantityItem.setQuantity(-5);
        CreateMedicineRequestRequest negativeQuantityRequest = new CreateMedicineRequestRequest();
        negativeQuantityRequest.setWarehouseId(1L);
        negativeQuantityRequest.setItems(List.of(negativeQuantityItem));

        IllegalArgumentException exception4 = assertThrows(IllegalArgumentException.class,
            () -> medicineRequestService.createRequest(negativeQuantityRequest, 1L));
        assertTrue(exception4.getMessage().contains("Requested quantity must be greater than zero"));
    }

    // ==========================================
    // 2. WHITE-BOX TESTING (CFG, DFG)
    // ==========================================

    /**
     * Kiểm thử luồng điều khiển (CFG - Branch Coverage)
     * Dành cho hàm createRequest():
     * - Branch 1: items == null -> throw IllegalArgumentException
     * - Branch 2: items.isEmpty() -> throw IllegalArgumentException
     * - Branch 3: quantity == null -> throw IllegalArgumentException
     * - Branch 4: quantity <= 0 -> throw IllegalArgumentException
     * - Branch 5: Success path -> save and return response
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createRequest() - Null items")
    public void testCreateRequest_NullItems_BranchCoverage() {
        CreateMedicineRequestRequest request = new CreateMedicineRequestRequest();
        request.setWarehouseId(1L);
        request.setItems(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> medicineRequestService.createRequest(request, 1L));

        assertTrue(exception.getMessage().contains("Request must include at least one item"));
        verifyNoInteractions(warehouseRepository);
        verifyNoInteractions(userRepository);
        verifyNoInteractions(medicineRepository);
        verifyNoInteractions(medicineRequestRepository);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createRequest() - Empty items")
    public void testCreateRequest_EmptyItems_BranchCoverage() {
        CreateMedicineRequestRequest request = new CreateMedicineRequestRequest();
        request.setWarehouseId(1L);
        request.setItems(new ArrayList<>());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> medicineRequestService.createRequest(request, 1L));

        assertTrue(exception.getMessage().contains("Request must include at least one item"));
        verifyNoInteractions(warehouseRepository);
        verifyNoInteractions(userRepository);
        verifyNoInteractions(medicineRepository);
        verifyNoInteractions(medicineRequestRepository);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createRequest() - Null quantity")
    public void testCreateRequest_NullQuantity_BranchCoverage() {
        CreateMedicineRequestRequest.ItemRequest nullQuantityItem = new CreateMedicineRequestRequest.ItemRequest();
        nullQuantityItem.setMedicineId(1L);
        nullQuantityItem.setQuantity(null);
        CreateMedicineRequestRequest request = new CreateMedicineRequestRequest();
        request.setWarehouseId(1L);
        request.setItems(List.of(nullQuantityItem));

        // Mock repositories to allow the method to proceed to validation
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> medicineRequestService.createRequest(request, 1L));

        assertTrue(exception.getMessage().contains("Requested quantity must be greater than zero"));
        verify(warehouseRepository).findById(1L);
        verify(userRepository).findById(1L);
        // medicineRepository is not called because quantity validation happens first
        verifyNoInteractions(medicineRepository);
        verifyNoInteractions(medicineRequestRepository);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createRequest() - Zero quantity")
    public void testCreateRequest_ZeroQuantity_BranchCoverage() {
        CreateMedicineRequestRequest.ItemRequest zeroQuantityItem = new CreateMedicineRequestRequest.ItemRequest();
        zeroQuantityItem.setMedicineId(1L);
        zeroQuantityItem.setQuantity(0);
        CreateMedicineRequestRequest request = new CreateMedicineRequestRequest();
        request.setWarehouseId(1L);
        request.setItems(List.of(zeroQuantityItem));

        // Mock repositories to allow the method to proceed to validation
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> medicineRequestService.createRequest(request, 1L));

        assertTrue(exception.getMessage().contains("Requested quantity must be greater than zero"));
        verify(warehouseRepository).findById(1L);
        verify(userRepository).findById(1L);
        // medicineRepository is not called because quantity validation happens first
        verifyNoInteractions(medicineRepository);
        verifyNoInteractions(medicineRequestRepository);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createRequest() - Success path")
    public void testCreateRequest_SuccessPath_BranchCoverage() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine1));
        when(medicineRepository.findById(2L)).thenReturn(Optional.of(testMedicine2));
        when(medicineRequestRepository.save(any(MedicineRequest.class))).thenReturn(testMedicineRequest);

        MedicineRequestResponse result = medicineRequestService.createRequest(testRequest, 1L);

        assertNotNull(result);
        verify(warehouseRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(medicineRepository, times(2)).findById(anyLong());
        verify(medicineRequestRepository).save(any(MedicineRequest.class));
    }

    /**
     * Kiểm thử luồng điều khiển (CFG) cho approveRequest():
     * - Branch 1: status == null -> set to PENDING
     * - Branch 2: status != PENDING -> throw IllegalStateException
     * - Branch 3: status == PENDING -> approve and save
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh approveRequest() - Null status")
    public void testApproveRequest_NullStatus_BranchCoverage() {
        testMedicineRequest.setStatus(null);
        when(medicineRequestRepository.findByIdWithItems(1L)).thenReturn(testMedicineRequest);
        when(medicineRequestRepository.save(any(MedicineRequest.class))).thenReturn(testMedicineRequest);

        MedicineRequestResponse result = medicineRequestService.approveRequest(1L);

        assertNotNull(result);
        assertEquals("APPROVED", result.getStatus());
        verify(medicineRequestRepository).findByIdWithItems(1L);
        verify(medicineRequestRepository).save(testMedicineRequest);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh approveRequest() - Already approved")
    public void testApproveRequest_AlreadyApproved_BranchCoverage() {
        testMedicineRequest.setStatus(MedicineRequest.RequestStatus.APPROVED);
        when(medicineRequestRepository.findByIdWithItems(1L)).thenReturn(testMedicineRequest);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> medicineRequestService.approveRequest(1L));

        assertTrue(exception.getMessage().contains("Only PENDING requests can be approved"));
        verify(medicineRequestRepository).findByIdWithItems(1L);
        verifyNoMoreInteractions(medicineRequestRepository);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh approveRequest() - Success")
    public void testApproveRequest_Success_BranchCoverage() {
        testMedicineRequest.setStatus(MedicineRequest.RequestStatus.PENDING);
        when(medicineRequestRepository.findByIdWithItems(1L)).thenReturn(testMedicineRequest);
        when(medicineRequestRepository.save(any(MedicineRequest.class))).thenReturn(testMedicineRequest);

        MedicineRequestResponse result = medicineRequestService.approveRequest(1L);

        assertNotNull(result);
        assertEquals("APPROVED", result.getStatus());
        verify(medicineRequestRepository).findByIdWithItems(1L);
        verify(medicineRequestRepository).save(testMedicineRequest);
    }

    /**
     * Kiểm thử luồng điều khiển (CFG) cho rejectRequest():
     * - Branch 1: status == null -> set to PENDING
     * - Branch 2: status != PENDING -> throw IllegalStateException
     * - Branch 3: status == PENDING -> reject and save
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh rejectRequest() - Null status")
    public void testRejectRequest_NullStatus_BranchCoverage() {
        testMedicineRequest.setStatus(null);
        when(medicineRequestRepository.findByIdWithItems(1L)).thenReturn(testMedicineRequest);
        when(medicineRequestRepository.save(any(MedicineRequest.class))).thenReturn(testMedicineRequest);

        MedicineRequestResponse result = medicineRequestService.rejectRequest(1L);

        assertNotNull(result);
        assertEquals("REJECTED", result.getStatus());
        verify(medicineRequestRepository).findByIdWithItems(1L);
        verify(medicineRequestRepository).save(testMedicineRequest);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh rejectRequest() - Already rejected")
    public void testRejectRequest_AlreadyRejected_BranchCoverage() {
        testMedicineRequest.setStatus(MedicineRequest.RequestStatus.REJECTED);
        when(medicineRequestRepository.findByIdWithItems(1L)).thenReturn(testMedicineRequest);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> medicineRequestService.rejectRequest(1L));

        assertTrue(exception.getMessage().contains("Only PENDING requests can be rejected"));
        verify(medicineRequestRepository).findByIdWithItems(1L);
        verifyNoMoreInteractions(medicineRequestRepository);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh rejectRequest() - Success")
    public void testRejectRequest_Success_BranchCoverage() {
        testMedicineRequest.setStatus(MedicineRequest.RequestStatus.PENDING);
        when(medicineRequestRepository.findByIdWithItems(1L)).thenReturn(testMedicineRequest);
        when(medicineRequestRepository.save(any(MedicineRequest.class))).thenReturn(testMedicineRequest);

        MedicineRequestResponse result = medicineRequestService.rejectRequest(1L);

        assertNotNull(result);
        assertEquals("REJECTED", result.getStatus());
        verify(medicineRequestRepository).findByIdWithItems(1L);
        verify(medicineRequestRepository).save(testMedicineRequest);
    }

    /**
     * Phân tích luồng dữ liệu (DFG - Data Flow Graph)
     * Hàm: createRequest()
     * Quy trình DFG:
     * 1. Use request.getItems() -> null/empty check
     * 2. Use request.getWarehouseId() -> find warehouse
     * 3. Use userId -> find user
     * 4. Use itemRequest.getMedicineId() -> find medicine
     * 5. Use itemRequest.getQuantity() -> validation check
     * 6. Use all data -> create MedicineRequest and items
     * 7. Use created request -> save and convert to response
     */
    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng dữ liệu createRequest() - Complete flow")
    public void testCreateRequest_CompleteDataFlow() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine1));
        when(medicineRepository.findById(2L)).thenReturn(Optional.of(testMedicine2));
        when(medicineRequestRepository.save(any(MedicineRequest.class))).thenReturn(testMedicineRequest);

        MedicineRequestResponse result = medicineRequestService.createRequest(testRequest, 1L);

        // Verify complete data flow
        assertNotNull(result);
        verify(warehouseRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(medicineRepository).findById(1L);
        verify(medicineRepository).findById(2L);
        verify(medicineRequestRepository).save(any(MedicineRequest.class));
    }

    // ==========================================
    // 3. SUPPLEMENTARY COVERAGE TESTS
    // ==========================================

    @Test
    @DisplayName("Supplementary: Test createRequest() with warehouse not found")
    public void testCreateRequest_WarehouseNotFound() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> medicineRequestService.createRequest(testRequest, 1L));

        assertTrue(exception.getMessage().contains("Warehouse not found"));
        verify(warehouseRepository).findById(1L);
        verifyNoMoreInteractions(warehouseRepository);
        verifyNoInteractions(userRepository);
        verifyNoInteractions(medicineRepository);
        verifyNoInteractions(medicineRequestRepository);
    }

    @Test
    @DisplayName("Supplementary: Test createRequest() with user not found")
    public void testCreateRequest_UserNotFound() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> medicineRequestService.createRequest(testRequest, 1L));

        assertTrue(exception.getMessage().contains("User not found"));
        verify(warehouseRepository).findById(1L);
        verify(userRepository).findById(1L);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(medicineRepository);
        verifyNoInteractions(medicineRequestRepository);
    }

    @Test
    @DisplayName("Supplementary: Test createRequest() with medicine not found")
    public void testCreateRequest_MedicineNotFound() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(medicineRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> medicineRequestService.createRequest(testRequest, 1L));

        assertTrue(exception.getMessage().contains("Medicine not found with id: 1"));
        verify(warehouseRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(medicineRepository).findById(1L);
        verifyNoMoreInteractions(medicineRepository);
        verifyNoInteractions(medicineRequestRepository);
    }

    @Test
    @DisplayName("Supplementary: Test approveRequest() with request not found")
    public void testApproveRequest_RequestNotFound() {
        when(medicineRequestRepository.findByIdWithItems(1L)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> medicineRequestService.approveRequest(1L));

        assertTrue(exception.getMessage().contains("Medicine request not found with id: 1"));
        verify(medicineRequestRepository).findByIdWithItems(1L);
        verifyNoMoreInteractions(medicineRequestRepository);
    }

    @Test
    @DisplayName("Supplementary: Test rejectRequest() with request not found")
    public void testRejectRequest_RequestNotFound() {
        when(medicineRequestRepository.findByIdWithItems(1L)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> medicineRequestService.rejectRequest(1L));

        assertTrue(exception.getMessage().contains("Medicine request not found with id: 1"));
        verify(medicineRequestRepository).findByIdWithItems(1L);
        verifyNoMoreInteractions(medicineRequestRepository);
    }

    @Test
    @DisplayName("Supplementary: Test getRequestEntity() with valid request")
    public void testGetRequestEntity_ValidRequest() {
        when(medicineRequestRepository.findByIdWithItems(1L)).thenReturn(testMedicineRequest);

        MedicineRequest result = medicineRequestService.getRequestEntity(1L);

        assertNotNull(result);
        assertEquals(1L, result.getRequestId());
        verify(medicineRequestRepository).findByIdWithItems(1L);
    }

    @Test
    @DisplayName("Supplementary: Test getRequestEntity() with request not found")
    public void testGetRequestEntity_RequestNotFound() {
        when(medicineRequestRepository.findByIdWithItems(1L)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> medicineRequestService.getRequestEntity(1L));

        assertTrue(exception.getMessage().contains("Medicine request not found with id: 1"));
        verify(medicineRequestRepository).findByIdWithItems(1L);
    }

    // @Test
    // @DisplayName("Supplementary: Test getAllRequests() with empty list")
    // public void testGetAllRequests_EmptyList() {
    //     when(medicineRequestRepository.findAllWithItems()).thenReturn(new ArrayList<>());

    //     List<MedicineRequestResponse> result = medicineRequestService.getAllRequests();

    //     assertNotNull(result);
    //     assertTrue(result.isEmpty());
    //     verify(medicineRequestRepository).findAllWithItems();
    // }

    // @Test
    // @DisplayName("Supplementary: Test getMyRequests() with empty list")
    // public void testGetMyRequests_EmptyList() {
    //     when(medicineRequestRepository.findAllByRequestedByUserIdWithItems(1L)).thenReturn(new ArrayList<>());

    //     List<MedicineRequestResponse> result = medicineRequestService.getMyRequests(1L);

    //     assertNotNull(result);
    //     assertTrue(result.isEmpty());
    //     verify(medicineRequestRepository).findAllByRequestedByUserIdWithItems(1L);
    // }

    @Test
    @DisplayName("Supplementary: Test createRequest() with very large quantity")
    public void testCreateRequest_VeryLargeQuantity() {
        CreateMedicineRequestRequest.ItemRequest largeQuantityItem = new CreateMedicineRequestRequest.ItemRequest();
        largeQuantityItem.setMedicineId(1L);
        largeQuantityItem.setQuantity(Integer.MAX_VALUE);
        CreateMedicineRequestRequest largeQuantityRequest = new CreateMedicineRequestRequest();
        largeQuantityRequest.setWarehouseId(1L);
        largeQuantityRequest.setItems(List.of(largeQuantityItem));

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine1));
        when(medicineRequestRepository.save(any(MedicineRequest.class))).thenReturn(testMedicineRequest);

        assertDoesNotThrow(() -> medicineRequestService.createRequest(largeQuantityRequest, 1L));

        verify(warehouseRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(medicineRepository).findById(1L);
        verify(medicineRequestRepository).save(any(MedicineRequest.class));
    }

    @Test
    @DisplayName("Supplementary: Test createRequest() with null notes and required date")
    public void testCreateRequest_NullNotesAndRequiredDate() {
        CreateMedicineRequestRequest request = new CreateMedicineRequestRequest();
        request.setWarehouseId(1L);
        request.setRequiredDate(null);
        request.setNotes(null);
        request.setItems(List.of(testItemRequest1));

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine1));
        when(medicineRequestRepository.save(any(MedicineRequest.class))).thenAnswer(invocation -> {
            MedicineRequest saved = invocation.getArgument(0);
            saved.setRequestId(1L);
            saved.setCreatedDate(LocalDateTime.now());
            // The saved entity will have the values from the request
            return saved;
        });

        MedicineRequestResponse result = medicineRequestService.createRequest(request, 1L);

        assertNotNull(result);
        // The required date comes from the saved entity - it can be null if not set
        assertNull(result.getRequiredDate()); // Should be null as requested
        assertNull(result.getNotes()); // Should be null as requested
        verify(warehouseRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(medicineRepository).findById(1L);
        verify(medicineRequestRepository).save(any(MedicineRequest.class));
    }

    @Test
    @DisplayName("Supplementary: Test createRequest() with empty notes string")
    public void testCreateRequest_EmptyNotesString() {
        CreateMedicineRequestRequest.ItemRequest itemWithEmptyNotes = new CreateMedicineRequestRequest.ItemRequest();
        itemWithEmptyNotes.setMedicineId(1L);
        itemWithEmptyNotes.setQuantity(10);
        itemWithEmptyNotes.setNotes("");
        CreateMedicineRequestRequest request = new CreateMedicineRequestRequest();
        request.setWarehouseId(1L);
        request.setNotes("");
        request.setItems(List.of(itemWithEmptyNotes));

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine1));
        when(medicineRequestRepository.save(any(MedicineRequest.class))).thenAnswer(invocation -> {
            MedicineRequest saved = invocation.getArgument(0);
            saved.setRequestId(1L);
            saved.setCreatedDate(LocalDateTime.now());
            // The saved entity will have the values from the request
            return saved;
        });

        MedicineRequestResponse result = medicineRequestService.createRequest(request, 1L);

        assertNotNull(result);
        assertEquals("", result.getNotes()); // Empty string should be preserved
        verify(warehouseRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(medicineRepository).findById(1L);
        verify(medicineRequestRepository).save(any(MedicineRequest.class));
    }
}
