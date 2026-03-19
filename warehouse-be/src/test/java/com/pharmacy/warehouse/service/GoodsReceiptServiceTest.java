package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.dto.ApproveGoodsReceiptRequest;
import com.pharmacy.warehouse.dto.CreateGoodsReceiptRequest;
import com.pharmacy.warehouse.dto.GoodsReceiptResponse;
import com.pharmacy.warehouse.dto.PurchaseOrderResponse;
import com.pharmacy.warehouse.model.*;
import com.pharmacy.warehouse.model.GoodsReceipt.ReceiptStatus;
import com.pharmacy.warehouse.model.PurchaseOrder.PurchaseOrderStatus;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GoodsReceiptServiceTest {

    @Mock
    private GoodsReceiptRepository goodsReceiptRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private PurchaseOrderService purchaseOrderService;

    @InjectMocks
    private GoodsReceiptService goodsReceiptService;

    private GoodsReceipt testGoodsReceipt;
    private PurchaseOrder testPurchaseOrder;
    private PurchaseOrderItem testPurchaseOrderItem;
    private User testUser;
    private User testApprover;
    private Warehouse testWarehouse;
    private Medicine testMedicine;
    private Batch testBatch;
    private CreateGoodsReceiptRequest testCreateGoodsReceiptRequest;
    private ApproveGoodsReceiptRequest testApproveGoodsReceiptRequest;
    private LocalDateTime testTime;

    @BeforeEach
    public void setup() {
        testTime = LocalDateTime.of(2023, 1, 1, 12, 0, 0);

        // Setup test warehouse
        testWarehouse = new Warehouse();
        testWarehouse.setWarehouseId(1L);
        testWarehouse.setName("Main Warehouse");
        testWarehouse.setLocation("Test Location");

        // Setup test medicine
        testMedicine = new Medicine();
        testMedicine.setMedicineId(1L);
        testMedicine.setName("Test Medicine");
        testMedicine.setManufacturer("Test Manufacturer");

        // Setup test user (warehouse staff)
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("staffuser");
        testUser.setFullName("Staff User");
        testUser.setEmail("staff@example.com");

        // Setup test approver (warehouse manager)
        testApprover = new User();
        testApprover.setUserId(2L);
        testApprover.setUsername("manageruser");
        testApprover.setFullName("Manager User");
        testApprover.setEmail("manager@example.com");

        // Setup test batch
        testBatch = new Batch();
        testBatch.setBatchId(1L);
        testBatch.setMedicine(testMedicine);
        testBatch.setWarehouse(testWarehouse);
        testBatch.setQuantity(100);
        testBatch.setLotNumber("LOT001");
        testBatch.setExpiryDate(LocalDate.of(2024, 12, 31));
        testBatch.setStatus("AVAILABLE");

        // Setup test purchase order item
        testPurchaseOrderItem = new PurchaseOrderItem();
        testPurchaseOrderItem.setItemId(1L);
        testPurchaseOrderItem.setMedicine(testMedicine);
        testPurchaseOrderItem.setRequestedQuantity(100);
        testPurchaseOrderItem.setReceivedQuantity(0);
        testPurchaseOrderItem.setUnitPrice(java.math.BigDecimal.valueOf(10.0));
        testPurchaseOrderItem.setTotalPrice(java.math.BigDecimal.valueOf(1000.0));
        testPurchaseOrderItem.setExpectedExpiryDate(LocalDate.of(2024, 12, 31));
        testPurchaseOrderItem.setActualExpiryDate(null);

        // Setup test purchase order
        testPurchaseOrder = new PurchaseOrder();
        testPurchaseOrder.setPurchaseOrderId(1L);
        testPurchaseOrder.setOrderCode("PO-2023-0001");
        testPurchaseOrder.setWarehouse(testWarehouse);
        testPurchaseOrder.setStatus(PurchaseOrderStatus.SHIPPING);
        testPurchaseOrder.setTotalAmount(java.math.BigDecimal.valueOf(1000.0));
        testPurchaseOrder.setExpectedDeliveryDate(LocalDate.of(2023, 1, 15));
        testPurchaseOrder.setNotes("Test notes");
        testPurchaseOrder.setCreatedAt(testTime);
        testPurchaseOrder.setUpdatedAt(testTime);
        testPurchaseOrder.setItems(List.of(testPurchaseOrderItem));

        // Setup test goods receipt
        testGoodsReceipt = new GoodsReceipt();
        testGoodsReceipt.setReceiptId(1L);
        testGoodsReceipt.setReceiptCode("GR-2023-0001");
        testGoodsReceipt.setPurchaseOrder(testPurchaseOrder);
        testGoodsReceipt.setReceivedBy(testUser);
        testGoodsReceipt.setApprovedBy(null);
        testGoodsReceipt.setStatus(ReceiptStatus.PENDING_APPROVAL);
        testGoodsReceipt.setQualityCheckNotes("Quality check passed");
        testGoodsReceipt.setQualityPassed(true);
        testGoodsReceipt.setReceivedAt(testTime);
        testGoodsReceipt.setApprovedAt(null);
        testGoodsReceipt.setCreatedAt(testTime);

        // Setup test create goods receipt request
        CreateGoodsReceiptRequest.ReceivedItemInfo itemInfo = 
            new CreateGoodsReceiptRequest.ReceivedItemInfo();
        itemInfo.setItemId(1L);
        itemInfo.setReceivedQuantity(100);
        itemInfo.setActualExpiryDate(LocalDate.of(2024, 12, 31));
        itemInfo.setLotNumber("LOT001");
        itemInfo.setManufactureDate(LocalDate.of(2022, 12, 31));

        testCreateGoodsReceiptRequest = new CreateGoodsReceiptRequest();
        testCreateGoodsReceiptRequest.setPurchaseOrderId(1L);
        testCreateGoodsReceiptRequest.setQualityCheckNotes("Quality check passed");
        testCreateGoodsReceiptRequest.setQualityPassed(true);
        testCreateGoodsReceiptRequest.setReceivedItems(List.of(itemInfo));

        // Setup test approve goods receipt request
        testApproveGoodsReceiptRequest = new ApproveGoodsReceiptRequest();
        testApproveGoodsReceiptRequest.setApproved(true);
        testApproveGoodsReceiptRequest.setNotes("Approved successfully");
    }

    // ==========================================
    // 1. BLACK-BOX TESTING (EP, BVA)
    // ==========================================

    /**
     * Kiểm thử Hộp đen: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
     * Target Hàm: getAllGoodsReceipts()
     * Phân tích kết quả trả về từ repository:
     * - Empty list -> trả về empty list
     * - Single item list -> trả về single item list  
     * - Multiple items list -> trả về multiple items list
     */
    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra getAllGoodsReceipts() với các trường hợp khác nhau")
    public void testGetAllGoodsReceipts_EquivalencePartition_BoundaryValue() {
        // Case 1: Empty list - Phân vùng rỗng
        when(goodsReceiptRepository.findAll()).thenReturn(new ArrayList<>());
        List<GoodsReceiptResponse> result1 = goodsReceiptService.getAllGoodsReceipts();
        assertTrue(result1.isEmpty());
        verify(goodsReceiptRepository, times(1)).findAll();

        // Case 2: Single item - Giá trị biên dưới
        when(goodsReceiptRepository.findAll()).thenReturn(List.of(testGoodsReceipt));
        List<GoodsReceiptResponse> result2 = goodsReceiptService.getAllGoodsReceipts();
        assertEquals(1, result2.size());
        assertEquals(1L, result2.get(0).getReceiptId());
        verify(goodsReceiptRepository, times(2)).findAll();

        // Case 3: Multiple items - Phân vùng bình thường
        GoodsReceipt receipt2 = new GoodsReceipt();
        receipt2.setReceiptId(2L);
        receipt2.setReceiptCode("GR-2023-0002");
        receipt2.setPurchaseOrder(testPurchaseOrder);
        receipt2.setReceivedBy(testUser);
        receipt2.setStatus(ReceiptStatus.APPROVED);
        
        when(goodsReceiptRepository.findAll()).thenReturn(List.of(testGoodsReceipt, receipt2));
        List<GoodsReceiptResponse> result3 = goodsReceiptService.getAllGoodsReceipts();
        assertEquals(2, result3.size());
        assertEquals(1L, result3.get(0).getReceiptId());
        assertEquals(2L, result3.get(1).getReceiptId());
        verify(goodsReceiptRepository, times(3)).findAll();
    }

    /**
     * Kiểm thử Hộp đen: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
     * Target Hàm: getPendingGoodsReceipts()
     * Phân tích kết quả trả về từ repository:
     * - Empty list -> trả về empty list
     * - Single item list -> trả về single item list
     * - Multiple items list -> trả về multiple items list
     */
    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra getPendingGoodsReceipts() với các trường hợp khác nhau")
    public void testGetPendingGoodsReceipts_EquivalencePartition_BoundaryValue() {
        // Case 1: Empty list - Phân vùng rỗng
        when(goodsReceiptRepository.findPendingReceipts()).thenReturn(new ArrayList<>());
        List<GoodsReceiptResponse> result1 = goodsReceiptService.getPendingGoodsReceipts();
        assertTrue(result1.isEmpty());
        verify(goodsReceiptRepository, times(1)).findPendingReceipts();

        // Case 2: Single item - Giá trị biên dưới
        when(goodsReceiptRepository.findPendingReceipts()).thenReturn(List.of(testGoodsReceipt));
        List<GoodsReceiptResponse> result2 = goodsReceiptService.getPendingGoodsReceipts();
        assertEquals(1, result2.size());
        assertEquals(1L, result2.get(0).getReceiptId());
        verify(goodsReceiptRepository, times(2)).findPendingReceipts();

        // Case 3: Multiple items - Phân vùng bình thường
        GoodsReceipt receipt2 = new GoodsReceipt();
        receipt2.setReceiptId(2L);
        receipt2.setReceiptCode("GR-2023-0002");
        receipt2.setPurchaseOrder(testPurchaseOrder);
        receipt2.setReceivedBy(testUser);
        receipt2.setStatus(ReceiptStatus.PENDING_APPROVAL);
        
        when(goodsReceiptRepository.findPendingReceipts()).thenReturn(List.of(testGoodsReceipt, receipt2));
        List<GoodsReceiptResponse> result3 = goodsReceiptService.getPendingGoodsReceipts();
        assertEquals(2, result3.size());
        assertEquals(1L, result3.get(0).getReceiptId());
        assertEquals(2L, result3.get(1).getReceiptId());
        verify(goodsReceiptRepository, times(3)).findPendingReceipts();
    }

    // ==========================================
    // 2. WHITE-BOX TESTING (CFG, DFG)
    // ==========================================

    /**
     * Kiểm thử luồng điều khiển (CFG - Branch Coverage)
     * Dành cho hàm getGoodsReceiptById(Long id):
     * - Branch 1: goodsReceiptRepository.findById() trả về Optional.isPresent() -> tiếp tục xử lý
     * - Branch 2: goodsReceiptRepository.findById() trả về Optional.isEmpty() -> ném RuntimeException
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getGoodsReceiptById() - Success path")
    public void testGetGoodsReceiptById_SuccessPath_BranchCoverage() {
        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));
        when(purchaseOrderService.getPurchaseOrderById(1L)).thenReturn(mock(PurchaseOrderResponse.class));

        GoodsReceiptResponse result = goodsReceiptService.getGoodsReceiptById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getReceiptId());
        assertEquals("GR-2023-0001", result.getReceiptCode());
        assertEquals("PENDING_APPROVAL", result.getStatus());
        assertEquals("Quality check passed", result.getQualityCheckNotes());
        assertTrue(result.getQualityPassed());
        assertEquals(testTime, result.getReceivedAt());
        assertNull(result.getApprovedAt());
        assertNotNull(result.getReceivedBy());
        assertNull(result.getApprovedBy());

        verify(goodsReceiptRepository, times(1)).findById(1L);
        verify(purchaseOrderService, times(1)).getPurchaseOrderById(1L);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getGoodsReceiptById() - Exception path")
    public void testGetGoodsReceiptById_NotFound_BranchCoverage() {
        when(goodsReceiptRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            goodsReceiptService.getGoodsReceiptById(999L);
        });

        assertEquals("Goods receipt not found with id: 999", exception.getMessage());
        verify(goodsReceiptRepository, times(1)).findById(999L);
        verify(purchaseOrderService, never()).getPurchaseOrderById(any());
    }

    /**
     * Kiểm thử luồng điều khiển cho getGoodsReceiptByPurchaseOrderId()
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getGoodsReceiptByPurchaseOrderId() - Success path")
    public void testGetGoodsReceiptByPurchaseOrderId_Success_BranchCoverage() {
        when(goodsReceiptRepository.findByPurchaseOrder_PurchaseOrderId(1L))
            .thenReturn(Optional.of(testGoodsReceipt));
        when(purchaseOrderService.getPurchaseOrderById(1L)).thenReturn(mock(PurchaseOrderResponse.class));

        GoodsReceiptResponse result = goodsReceiptService.getGoodsReceiptByPurchaseOrderId(1L);

        assertNotNull(result);
        assertEquals(1L, result.getReceiptId());
        assertEquals("GR-2023-0001", result.getReceiptCode());

        verify(goodsReceiptRepository, times(1)).findByPurchaseOrder_PurchaseOrderId(1L);
        verify(purchaseOrderService, times(1)).getPurchaseOrderById(1L);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getGoodsReceiptByPurchaseOrderId() - Exception path")
    public void testGetGoodsReceiptByPurchaseOrderId_NotFound_BranchCoverage() {
        when(goodsReceiptRepository.findByPurchaseOrder_PurchaseOrderId(999L))
            .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            goodsReceiptService.getGoodsReceiptByPurchaseOrderId(999L);
        });

        assertEquals("Goods receipt not found for purchase order: 999", exception.getMessage());
        verify(goodsReceiptRepository, times(1)).findByPurchaseOrder_PurchaseOrderId(999L);
        verify(purchaseOrderService, never()).getPurchaseOrderById(any());
    }

    /**
     * Phân tích luồng dữ liệu (DFG - Data Flow Graph)
     * Hàm: createGoodsReceipt(CreateGoodsReceiptRequest request, Long userId)
     * Quy trình DFG:
     * 1. Define parameters `request`, `userId` -> Use trong validation
     * 2. Use request.getPurchaseOrderId() -> repository.findByIdWithItems() -> Define purchaseOrder
     * 3. Use purchaseOrder.getStatus() -> status validation
     * 4. Use request.getPurchaseOrderId() -> check existing receipt
     * 5. Use userId -> repository.findById() -> Define user
     * 6. Use request.getReceivedItems() -> loop qua items -> Update purchase order items
     * 7. Use receipt -> repository.save() -> Define savedReceipt
     * 8. Use savedReceipt -> convertToResponse() -> return result
     */
    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng dữ liệu createGoodsReceipt() đầy đủ")
    public void testCreateGoodsReceipt_CompleteDataFlow() {
        when(purchaseOrderRepository.findByIdWithItems(1L)).thenReturn(testPurchaseOrder);
        when(goodsReceiptRepository.findByPurchaseOrder_PurchaseOrderId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(goodsReceiptRepository.save(any(GoodsReceipt.class))).thenAnswer(invocation -> {
            GoodsReceipt saved = invocation.getArgument(0);
            if (saved.getReceiptId() == null) {
                saved.setReceiptId(1L);
                saved.setReceiptCode("GR-2023-0001");
                saved.setCreatedAt(testTime);
            }
            return saved;
        });
        when(purchaseOrderService.getPurchaseOrderById(1L)).thenReturn(mock(PurchaseOrderResponse.class));

        GoodsReceiptResponse result = goodsReceiptService.createGoodsReceipt(testCreateGoodsReceiptRequest, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getReceiptId());
        assertEquals("GR-2023-0001", result.getReceiptCode());
        assertEquals("PENDING_APPROVAL", result.getStatus());
        assertEquals("Quality check passed", result.getQualityCheckNotes());
        assertTrue(result.getQualityPassed());
        assertNotNull(result.getReceivedBy());
        assertNull(result.getApprovedBy());

        // Verify purchase order item was updated
        assertEquals(Integer.valueOf(100), testPurchaseOrderItem.getReceivedQuantity());
        assertEquals(LocalDate.of(2024, 12, 31), testPurchaseOrderItem.getActualExpiryDate());

        // Verify purchase order status was updated
        assertEquals(PurchaseOrderStatus.RECEIVED, testPurchaseOrder.getStatus());

        verify(purchaseOrderRepository, times(1)).findByIdWithItems(1L);
        verify(goodsReceiptRepository, times(1)).findByPurchaseOrder_PurchaseOrderId(1L);
        verify(userRepository, times(1)).findById(1L);
        verify(purchaseOrderRepository, times(1)).save(testPurchaseOrder);
        verify(goodsReceiptRepository, times(1)).save(any(GoodsReceipt.class));
        verify(purchaseOrderService, times(1)).getPurchaseOrderById(1L);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createGoodsReceipt() - Purchase order not found")
    public void testCreateGoodsReceipt_PurchaseOrderNotFound_BranchCoverage() {
        when(purchaseOrderRepository.findByIdWithItems(999L)).thenReturn(null);

        CreateGoodsReceiptRequest request = new CreateGoodsReceiptRequest();
        request.setPurchaseOrderId(999L);
        request.setReceivedItems(new ArrayList<>());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            goodsReceiptService.createGoodsReceipt(request, 1L);
        });

        assertEquals("Purchase order not found", exception.getMessage());
        verify(purchaseOrderRepository, times(1)).findByIdWithItems(999L);
        verify(goodsReceiptRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createGoodsReceipt() - Invalid purchase order status")
    public void testCreateGoodsReceipt_InvalidPurchaseOrderStatus_BranchCoverage() {
        testPurchaseOrder.setStatus(PurchaseOrderStatus.PENDING);
        when(purchaseOrderRepository.findByIdWithItems(1L)).thenReturn(testPurchaseOrder);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            goodsReceiptService.createGoodsReceipt(testCreateGoodsReceiptRequest, 1L);
        });

        assertEquals("Purchase order must be in SHIPPING or CONFIRMED status", exception.getMessage());
        verify(purchaseOrderRepository, times(1)).findByIdWithItems(1L);
        verify(goodsReceiptRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createGoodsReceipt() - Goods receipt already exists")
    public void testCreateGoodsReceipt_ReceiptAlreadyExists_BranchCoverage() {
        when(purchaseOrderRepository.findByIdWithItems(1L)).thenReturn(testPurchaseOrder);
        when(goodsReceiptRepository.findByPurchaseOrder_PurchaseOrderId(1L))
            .thenReturn(Optional.of(testGoodsReceipt));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            goodsReceiptService.createGoodsReceipt(testCreateGoodsReceiptRequest, 1L);
        });

        assertEquals("Goods receipt already exists for this purchase order", exception.getMessage());
        verify(purchaseOrderRepository, times(1)).findByIdWithItems(1L);
        verify(goodsReceiptRepository, times(1)).findByPurchaseOrder_PurchaseOrderId(1L);
        verify(goodsReceiptRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createGoodsReceipt() - User not found")
    public void testCreateGoodsReceipt_UserNotFound_BranchCoverage() {
        when(purchaseOrderRepository.findByIdWithItems(1L)).thenReturn(testPurchaseOrder);
        when(goodsReceiptRepository.findByPurchaseOrder_PurchaseOrderId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            goodsReceiptService.createGoodsReceipt(testCreateGoodsReceiptRequest, 999L);
        });

        assertEquals("User not found", exception.getMessage());
        verify(purchaseOrderRepository, times(1)).findByIdWithItems(1L);
        verify(goodsReceiptRepository, times(1)).findByPurchaseOrder_PurchaseOrderId(1L);
        verify(userRepository, times(1)).findById(999L);
        verify(goodsReceiptRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createGoodsReceipt() - Purchase order item not found")
    public void testCreateGoodsReceipt_PurchaseOrderItemNotFound_BranchCoverage() {
        CreateGoodsReceiptRequest.ReceivedItemInfo itemInfo = 
            new CreateGoodsReceiptRequest.ReceivedItemInfo();
        itemInfo.setItemId(999L);
        itemInfo.setReceivedQuantity(100);

        CreateGoodsReceiptRequest request = new CreateGoodsReceiptRequest();
        request.setPurchaseOrderId(1L);
        request.setReceivedItems(List.of(itemInfo));

        when(purchaseOrderRepository.findByIdWithItems(1L)).thenReturn(testPurchaseOrder);
        when(goodsReceiptRepository.findByPurchaseOrder_PurchaseOrderId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            goodsReceiptService.createGoodsReceipt(request, 1L);
        });

        assertEquals("Purchase order item not found: 999", exception.getMessage());
        verify(purchaseOrderRepository, times(1)).findByIdWithItems(1L);
        verify(goodsReceiptRepository, times(1)).findByPurchaseOrder_PurchaseOrderId(1L);
        verify(userRepository, times(1)).findById(1L);
        verify(goodsReceiptRepository, never()).save(any());
    }

    /**
     * Kiểm thử luồng điều khiển cho approveGoodsReceipt()
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh approveGoodsReceipt() - Approve path")
    public void testApproveGoodsReceipt_Approve_BranchCoverage() {
        testGoodsReceipt.setStatus(ReceiptStatus.PENDING_APPROVAL);
        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testApprover));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(goodsReceiptRepository.save(any(GoodsReceipt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseOrderService.getPurchaseOrderById(1L)).thenReturn(mock(PurchaseOrderResponse.class));

        GoodsReceiptResponse result = goodsReceiptService.approveGoodsReceipt(1L, testApproveGoodsReceiptRequest, 2L);

        assertNotNull(result);
        assertEquals("APPROVED", result.getStatus());
        assertNotNull(result.getApprovedBy());
        assertNotNull(result.getApprovedAt());

        // Verify purchase order status was updated
        assertEquals(PurchaseOrderStatus.APPROVED, testPurchaseOrder.getStatus());

        verify(goodsReceiptRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(2L);
        verify(purchaseOrderRepository, times(1)).save(testPurchaseOrder);
        verify(goodsReceiptRepository, times(1)).save(testGoodsReceipt);
        verify(purchaseOrderService, times(1)).getPurchaseOrderById(1L);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh approveGoodsReceipt() - Reject path")
    public void testApproveGoodsReceipt_Reject_BranchCoverage() {
        testGoodsReceipt.setStatus(ReceiptStatus.PENDING_APPROVAL);
        ApproveGoodsReceiptRequest rejectRequest = new ApproveGoodsReceiptRequest();
        rejectRequest.setApproved(false);
        rejectRequest.setNotes("Quality issues found");

        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testApprover));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(goodsReceiptRepository.save(any(GoodsReceipt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseOrderService.getPurchaseOrderById(1L)).thenReturn(mock(PurchaseOrderResponse.class));

        GoodsReceiptResponse result = goodsReceiptService.approveGoodsReceipt(1L, rejectRequest, 2L);

        assertNotNull(result);
        assertEquals("REJECTED", result.getStatus());
        assertNotNull(result.getApprovedBy());
        assertNotNull(result.getApprovedAt());

        // Verify purchase order status was reverted
        assertEquals(PurchaseOrderStatus.SHIPPING, testPurchaseOrder.getStatus());

        verify(goodsReceiptRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(2L);
        verify(purchaseOrderRepository, times(1)).save(testPurchaseOrder);
        verify(goodsReceiptRepository, times(1)).save(testGoodsReceipt);
        verify(purchaseOrderService, times(1)).getPurchaseOrderById(1L);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh approveGoodsReceipt() - Receipt not found")
    public void testApproveGoodsReceipt_NotFound_BranchCoverage() {
        when(goodsReceiptRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            goodsReceiptService.approveGoodsReceipt(999L, testApproveGoodsReceiptRequest, 2L);
        });

        assertEquals("Goods receipt not found", exception.getMessage());
        verify(goodsReceiptRepository, times(1)).findById(999L);
        verify(goodsReceiptRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh approveGoodsReceipt() - Invalid status")
    public void testApproveGoodsReceipt_InvalidStatus_BranchCoverage() {
        testGoodsReceipt.setStatus(ReceiptStatus.APPROVED);
        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            goodsReceiptService.approveGoodsReceipt(1L, testApproveGoodsReceiptRequest, 2L);
        });

        assertEquals("Goods receipt must be in PENDING_APPROVAL status", exception.getMessage());
        verify(goodsReceiptRepository, times(1)).findById(1L);
        verify(goodsReceiptRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh approveGoodsReceipt() - User not found")
    public void testApproveGoodsReceipt_UserNotFound_BranchCoverage() {
        testGoodsReceipt.setStatus(ReceiptStatus.PENDING_APPROVAL);
        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            goodsReceiptService.approveGoodsReceipt(1L, testApproveGoodsReceiptRequest, 999L);
        });

        assertEquals("User not found", exception.getMessage());
        verify(goodsReceiptRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(999L);
        verify(goodsReceiptRepository, never()).save(any());
    }

    // ==========================================
    // 3. PRIVATE METHOD TESTING
    // ==========================================

    /**
     * Test private method createBatchesFromReceipt() thông qua approveGoodsReceipt()
     */
    @Test
    @DisplayName("Private Method | createBatchesFromReceipt: Test batch creation")
    public void testCreateBatchesFromReceipt_BatchCreation() {
        testGoodsReceipt.setStatus(ReceiptStatus.PENDING_APPROVAL);
        testPurchaseOrderItem.setReceivedQuantity(100);
        testPurchaseOrderItem.setActualExpiryDate(LocalDate.of(2024, 12, 31));

        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testApprover));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(goodsReceiptRepository.save(any(GoodsReceipt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> {
            Batch saved = invocation.getArgument(0);
            if (saved.getBatchId() == null) {
                saved.setBatchId(1L);
            }
            return saved;
        });
        when(purchaseOrderService.getPurchaseOrderById(1L)).thenReturn(mock(PurchaseOrderResponse.class));

        goodsReceiptService.approveGoodsReceipt(1L, testApproveGoodsReceiptRequest, 2L);

        // Verify batch was created
        verify(batchRepository, times(1)).save(any(Batch.class));
    }

    @Test
    @DisplayName("Private Method | createBatchesFromReceipt: Test no batch creation for zero quantity")
    public void testCreateBatchesFromReceipt_NoBatchCreationForZeroQuantity() {
        testGoodsReceipt.setStatus(ReceiptStatus.PENDING_APPROVAL);
        testPurchaseOrderItem.setReceivedQuantity(0); // Zero quantity
        testPurchaseOrderItem.setActualExpiryDate(LocalDate.of(2024, 12, 31));

        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testApprover));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(goodsReceiptRepository.save(any(GoodsReceipt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseOrderService.getPurchaseOrderById(1L)).thenReturn(mock(PurchaseOrderResponse.class));

        goodsReceiptService.approveGoodsReceipt(1L, testApproveGoodsReceiptRequest, 2L);

        // Verify no batch was created
        verify(batchRepository, never()).save(any(Batch.class));
    }

    @Test
    @DisplayName("Private Method | createBatchesFromReceipt: Test null received quantity")
    public void testCreateBatchesFromReceipt_NullReceivedQuantity() {
        testGoodsReceipt.setStatus(ReceiptStatus.PENDING_APPROVAL);
        testPurchaseOrderItem.setReceivedQuantity(null); // Null quantity
        testPurchaseOrderItem.setActualExpiryDate(LocalDate.of(2024, 12, 31));

        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testApprover));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(goodsReceiptRepository.save(any(GoodsReceipt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseOrderService.getPurchaseOrderById(1L)).thenReturn(mock(PurchaseOrderResponse.class));

        goodsReceiptService.approveGoodsReceipt(1L, testApproveGoodsReceiptRequest, 2L);

        // Verify no batch was created
        verify(batchRepository, never()).save(any(Batch.class));
    }

    @Test
    @DisplayName("Private Method | createBatchesFromReceipt: Test null expiry date")
    public void testCreateBatchesFromReceipt_NullExpiryDate() {
        testGoodsReceipt.setStatus(ReceiptStatus.PENDING_APPROVAL);
        testPurchaseOrderItem.setReceivedQuantity(100);
        testPurchaseOrderItem.setActualExpiryDate(null); // Null expiry date

        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testApprover));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(goodsReceiptRepository.save(any(GoodsReceipt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> {
            Batch saved = invocation.getArgument(0);
            if (saved.getBatchId() == null) {
                saved.setBatchId(1L);
            }
            return saved;
        });
        when(purchaseOrderService.getPurchaseOrderById(1L)).thenReturn(mock(PurchaseOrderResponse.class));

        goodsReceiptService.approveGoodsReceipt(1L, testApproveGoodsReceiptRequest, 2L);

        // Verify batch was created without manufacture date
        verify(batchRepository, times(1)).save(any(Batch.class));
    }

    /**
     * Test private method convertToResponse() thông qua các public methods
     */
    @Test
    @DisplayName("Private Method | convertToResponse: Test complete field mapping")
    public void testConvertToResponse_CompleteFieldMapping() {
        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));
        when(purchaseOrderService.getPurchaseOrderById(1L)).thenReturn(mock(PurchaseOrderResponse.class));

        GoodsReceiptResponse result = goodsReceiptService.getGoodsReceiptById(1L);

        assertNotNull(result);
        assertEquals(testGoodsReceipt.getReceiptId(), result.getReceiptId());
        assertEquals(testGoodsReceipt.getReceiptCode(), result.getReceiptCode());
        assertEquals(testGoodsReceipt.getStatus().name(), result.getStatus());
        assertEquals(testGoodsReceipt.getQualityCheckNotes(), result.getQualityCheckNotes());
        assertEquals(testGoodsReceipt.getQualityPassed(), result.getQualityPassed());
        assertEquals(testGoodsReceipt.getReceivedAt(), result.getReceivedAt());
        assertEquals(testGoodsReceipt.getApprovedAt(), result.getApprovedAt());
        assertEquals(testGoodsReceipt.getCreatedAt(), result.getCreatedAt());

        // Verify user info
        assertNotNull(result.getReceivedBy());
        assertEquals(testUser.getUserId(), result.getReceivedBy().getUserId());
        assertEquals(testUser.getUsername(), result.getReceivedBy().getUsername());
        assertEquals(testUser.getFullName(), result.getReceivedBy().getFullName());

        // Verify approved by is null
        assertNull(result.getApprovedBy());

        verify(goodsReceiptRepository, times(1)).findById(1L);
        verify(purchaseOrderService, times(1)).getPurchaseOrderById(1L);
    }

    @Test
    @DisplayName("Private Method | convertToResponse: Test with null purchase order")
    public void testConvertToResponse_NullPurchaseOrder() {
        testGoodsReceipt.setPurchaseOrder(null);
        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));

        GoodsReceiptResponse result = goodsReceiptService.getGoodsReceiptById(1L);

        assertNotNull(result);
        assertNull(result.getPurchaseOrder());

        verify(goodsReceiptRepository, times(1)).findById(1L);
        verify(purchaseOrderService, never()).getPurchaseOrderById(any());
    }

    @Test
    @DisplayName("Private Method | convertToResponse: Test with null received by")
    public void testConvertToResponse_NullReceivedBy() {
        testGoodsReceipt.setReceivedBy(null);
        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));
        when(purchaseOrderService.getPurchaseOrderById(1L)).thenReturn(mock(PurchaseOrderResponse.class));

        GoodsReceiptResponse result = goodsReceiptService.getGoodsReceiptById(1L);

        assertNotNull(result);
        assertNull(result.getReceivedBy());

        verify(goodsReceiptRepository, times(1)).findById(1L);
        verify(purchaseOrderService, times(1)).getPurchaseOrderById(1L);
    }

    @Test
    @DisplayName("Private Method | convertToResponse: Test with approved by")
    public void testConvertToResponse_WithApprovedBy() {
        testGoodsReceipt.setApprovedBy(testApprover);
        testGoodsReceipt.setApprovedAt(testTime);
        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));
        when(purchaseOrderService.getPurchaseOrderById(1L)).thenReturn(mock(PurchaseOrderResponse.class));

        GoodsReceiptResponse result = goodsReceiptService.getGoodsReceiptById(1L);

        assertNotNull(result);
        assertNotNull(result.getApprovedBy());
        assertEquals(testApprover.getUserId(), result.getApprovedBy().getUserId());
        assertEquals(testApprover.getUsername(), result.getApprovedBy().getUsername());
        assertEquals(testApprover.getFullName(), result.getApprovedBy().getFullName());

        verify(goodsReceiptRepository, times(1)).findById(1L);
        verify(purchaseOrderService, times(1)).getPurchaseOrderById(1L);
    }

    // ==========================================
    // 4. SUPPLEMENTARY COVERAGE TESTS
    // ==========================================

    @Test
    @DisplayName("Supplementary: Test createGoodsReceipt() with empty received items")
    public void testCreateGoodsReceipt_EmptyReceivedItems() {
        CreateGoodsReceiptRequest request = new CreateGoodsReceiptRequest();
        request.setPurchaseOrderId(1L);
        request.setReceivedItems(new ArrayList<>()); // Empty items

        when(purchaseOrderRepository.findByIdWithItems(1L)).thenReturn(testPurchaseOrder);
        when(goodsReceiptRepository.findByPurchaseOrder_PurchaseOrderId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(goodsReceiptRepository.save(any(GoodsReceipt.class))).thenAnswer(invocation -> {
            GoodsReceipt saved = invocation.getArgument(0);
            if (saved.getReceiptId() == null) {
                saved.setReceiptId(1L);
                saved.setReceiptCode("GR-2023-0001");
                saved.setCreatedAt(testTime);
            }
            return saved;
        });
        when(purchaseOrderService.getPurchaseOrderById(1L)).thenReturn(mock(PurchaseOrderResponse.class));

        GoodsReceiptResponse result = goodsReceiptService.createGoodsReceipt(request, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getReceiptId());

        verify(purchaseOrderRepository, times(1)).findByIdWithItems(1L);
        verify(goodsReceiptRepository, times(1)).findByPurchaseOrder_PurchaseOrderId(1L);
        verify(userRepository, times(1)).findById(1L);
        verify(purchaseOrderRepository, times(1)).save(testPurchaseOrder);
        verify(goodsReceiptRepository, times(1)).save(any(GoodsReceipt.class));
    }

    @Test
    @DisplayName("Supplementary: Test approveGoodsReceipt() with null notes")
    public void testApproveGoodsReceipt_NullNotes() {
        testGoodsReceipt.setStatus(ReceiptStatus.PENDING_APPROVAL);
        ApproveGoodsReceiptRequest request = new ApproveGoodsReceiptRequest();
        request.setApproved(true);
        request.setNotes(null); // Null notes

        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testApprover));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(goodsReceiptRepository.save(any(GoodsReceipt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseOrderService.getPurchaseOrderById(1L)).thenReturn(mock(PurchaseOrderResponse.class));

        GoodsReceiptResponse result = goodsReceiptService.approveGoodsReceipt(1L, request, 2L);

        assertNotNull(result);
        assertEquals("APPROVED", result.getStatus());

        verify(goodsReceiptRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(2L);
        verify(purchaseOrderRepository, times(1)).save(testPurchaseOrder);
        verify(goodsReceiptRepository, times(1)).save(testGoodsReceipt);
    }

    @Test
    @DisplayName("Supplementary: Test approveGoodsReceipt() with empty notes")
    public void testApproveGoodsReceipt_EmptyNotes() {
        testGoodsReceipt.setStatus(ReceiptStatus.PENDING_APPROVAL);
        ApproveGoodsReceiptRequest request = new ApproveGoodsReceiptRequest();
        request.setApproved(true);
        request.setNotes(""); // Empty notes

        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testApprover));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(goodsReceiptRepository.save(any(GoodsReceipt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseOrderService.getPurchaseOrderById(1L)).thenReturn(mock(PurchaseOrderResponse.class));

        GoodsReceiptResponse result = goodsReceiptService.approveGoodsReceipt(1L, request, 2L);

        assertNotNull(result);
        assertEquals("APPROVED", result.getStatus());

        verify(goodsReceiptRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(2L);
        verify(purchaseOrderRepository, times(1)).save(testPurchaseOrder);
        verify(goodsReceiptRepository, times(1)).save(testGoodsReceipt);
    }

    @Test
    @DisplayName("Supplementary: Test getAllGoodsReceipts repository exception")
    public void testGetAllGoodsReceiptsRepositoryException() {
        when(goodsReceiptRepository.findAll()).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            goodsReceiptService.getAllGoodsReceipts();
        });
    }

    @Test
    @DisplayName("Supplementary: Test getGoodsReceiptById repository exception")
    public void testGetGoodsReceiptByIdRepositoryException() {
        when(goodsReceiptRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            goodsReceiptService.getGoodsReceiptById(1L);
        });
    }

    @Test
    @DisplayName("Supplementary: Test getPendingGoodsReceipts repository exception")
    public void testGetPendingGoodsReceiptsRepositoryException() {
        when(goodsReceiptRepository.findPendingReceipts()).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            goodsReceiptService.getPendingGoodsReceipts();
        });
    }

    @Test
    @DisplayName("Supplementary: Test getGoodsReceiptByPurchaseOrderId repository exception")
    public void testGetGoodsReceiptByPurchaseOrderIdRepositoryException() {
        when(goodsReceiptRepository.findByPurchaseOrder_PurchaseOrderId(1L))
            .thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            goodsReceiptService.getGoodsReceiptByPurchaseOrderId(1L);
        });
    }

    @Test
    @DisplayName("Supplementary: Test createGoodsReceipt repository exception")
    public void testCreateGoodsReceiptRepositoryException() {
        when(purchaseOrderRepository.findByIdWithItems(1L)).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            goodsReceiptService.createGoodsReceipt(testCreateGoodsReceiptRequest, 1L);
        });
    }

    @Test
    @DisplayName("Supplementary: Test approveGoodsReceipt repository exception")
    public void testApproveGoodsReceiptRepositoryException() {
        when(goodsReceiptRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            goodsReceiptService.approveGoodsReceipt(1L, testApproveGoodsReceiptRequest, 2L);
        });
    }

    @Test
    @DisplayName("Supplementary: Test createGoodsReceipt() save exception")
    public void testCreateGoodsReceipt_SaveException() {
        when(purchaseOrderRepository.findByIdWithItems(1L)).thenReturn(testPurchaseOrder);
        when(goodsReceiptRepository.findByPurchaseOrder_PurchaseOrderId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenThrow(new RuntimeException("Save failed"));

        assertThrows(RuntimeException.class, () -> {
            goodsReceiptService.createGoodsReceipt(testCreateGoodsReceiptRequest, 1L);
        });

        verify(purchaseOrderRepository, times(1)).findByIdWithItems(1L);
        verify(goodsReceiptRepository, times(1)).findByPurchaseOrder_PurchaseOrderId(1L);
        verify(userRepository, times(1)).findById(1L);
        verify(purchaseOrderRepository, times(1)).save(any(PurchaseOrder.class));
    }

    @Test
    @DisplayName("Supplementary: Test approveGoodsReceipt() save exception")
    public void testApproveGoodsReceipt_SaveException() {
        testGoodsReceipt.setStatus(ReceiptStatus.PENDING_APPROVAL);
        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testApprover));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenThrow(new RuntimeException("Save failed"));

        assertThrows(RuntimeException.class, () -> {
            goodsReceiptService.approveGoodsReceipt(1L, testApproveGoodsReceiptRequest, 2L);
        });

        verify(goodsReceiptRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(2L);
        verify(purchaseOrderRepository, times(1)).save(any(PurchaseOrder.class));
    }

    @Test
    @DisplayName("Supplementary: Test createGoodsReceipt() with null optional fields")
    public void testCreateGoodsReceipt_NullOptionalFields() {
        CreateGoodsReceiptRequest request = new CreateGoodsReceiptRequest();
        request.setPurchaseOrderId(1L);
        request.setQualityCheckNotes(null); // Null notes
        request.setQualityPassed(null); // Null quality passed
        request.setReceivedItems(new ArrayList<>());

        when(purchaseOrderRepository.findByIdWithItems(1L)).thenReturn(testPurchaseOrder);
        when(goodsReceiptRepository.findByPurchaseOrder_PurchaseOrderId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(goodsReceiptRepository.save(any(GoodsReceipt.class))).thenAnswer(invocation -> {
            GoodsReceipt saved = invocation.getArgument(0);
            if (saved.getReceiptId() == null) {
                saved.setReceiptId(1L);
                saved.setReceiptCode("GR-2023-0001");
                saved.setCreatedAt(testTime);
            }
            return saved;
        });
        when(purchaseOrderService.getPurchaseOrderById(1L)).thenReturn(mock(PurchaseOrderResponse.class));

        GoodsReceiptResponse result = goodsReceiptService.createGoodsReceipt(request, 1L);

        assertNotNull(result);
        assertNull(result.getQualityCheckNotes());
        assertNull(result.getQualityPassed());

        verify(purchaseOrderRepository, times(1)).findByIdWithItems(1L);
        verify(goodsReceiptRepository, times(1)).findByPurchaseOrder_PurchaseOrderId(1L);
        verify(userRepository, times(1)).findById(1L);
        verify(purchaseOrderRepository, times(1)).save(testPurchaseOrder);
        verify(goodsReceiptRepository, times(1)).save(any(GoodsReceipt.class));
    }
}
