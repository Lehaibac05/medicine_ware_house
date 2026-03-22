package com.pharmacy.warehouse.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pharmacy.warehouse.dto.CreatePurchaseOrderRequest;
import com.pharmacy.warehouse.dto.PurchaseOrderResponse;
import com.pharmacy.warehouse.dto.SupplierResponse;
import com.pharmacy.warehouse.model.Medicine;
import com.pharmacy.warehouse.model.PurchaseOrder;
import com.pharmacy.warehouse.model.PurchaseOrder.PurchaseOrderStatus;
import com.pharmacy.warehouse.model.PurchaseOrderItem;
import com.pharmacy.warehouse.model.Supplier;
import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.model.Warehouse;
import com.pharmacy.warehouse.repository.MedicineRepository;
import com.pharmacy.warehouse.repository.PurchaseOrderRepository;
import com.pharmacy.warehouse.repository.SupplierRepository;
import com.pharmacy.warehouse.repository.UserRepository;
import com.pharmacy.warehouse.repository.WarehouseRepository;

@ExtendWith(MockitoExtension.class)
public class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    private PurchaseOrder testPurchaseOrder;
    private PurchaseOrderItem testPurchaseOrderItem;
    private Supplier testSupplier;
    private Warehouse testWarehouse;
    private Medicine testMedicine;
    private User testUser;
    private CreatePurchaseOrderRequest testCreatePurchaseOrderRequest;
    private LocalDate testDate;

    @BeforeEach
    public void setup() {
        testDate = LocalDate.of(2023, 1, 1);

        // Setup test user
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("testuser");
        testUser.setFullName("Test User");
        testUser.setEmail("test@example.com");

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

        // Setup test supplier
        testSupplier = new Supplier();
        testSupplier.setSupplierId(1L);
        testSupplier.setSupplierName("Test Supplier");
        testSupplier.setContactPerson("John Doe");
        testSupplier.setPhoneNumber("1234567890");
        testSupplier.setEmail("supplier@test.com");
        testSupplier.setAddress("123 Supplier St");
        testSupplier.setTaxCode("TAX123");
        testSupplier.setStatus(Supplier.SupplierStatus.ACTIVE);
        testSupplier.setCreatedAt(LocalDateTime.now());
        testSupplier.setUpdatedAt(LocalDateTime.now());

        // Setup test purchase order
        testPurchaseOrder = new PurchaseOrder();
        testPurchaseOrder.setPurchaseOrderId(1L);
        testPurchaseOrder.setOrderCode("PO-2023-0001");
        testPurchaseOrder.setSupplier(testSupplier);
        testPurchaseOrder.setWarehouse(testWarehouse);
        testPurchaseOrder.setCreatedBy(testUser);
        testPurchaseOrder.setStatus(PurchaseOrderStatus.PENDING);
        testPurchaseOrder.setExpectedDeliveryDate(testDate);
        testPurchaseOrder.setTotalAmount(BigDecimal.valueOf(1000.0));
        testPurchaseOrder.setNotes("Test notes");
        testPurchaseOrder.setCreatedAt(LocalDateTime.now());
        testPurchaseOrder.setUpdatedAt(LocalDateTime.now());

        // Setup test purchase order item
        testPurchaseOrderItem = new PurchaseOrderItem();
        testPurchaseOrderItem.setItemId(1L);
        testPurchaseOrderItem.setPurchaseOrder(testPurchaseOrder);
        testPurchaseOrderItem.setMedicine(testMedicine);
        testPurchaseOrderItem.setRequestedQuantity(10);
        testPurchaseOrderItem.setReceivedQuantity(0);
        testPurchaseOrderItem.setUnitPrice(BigDecimal.valueOf(100.0));
        testPurchaseOrderItem.setTotalPrice(BigDecimal.valueOf(1000.0));
        testPurchaseOrderItem.setExpectedExpiryDate(testDate.plusMonths(6));
        testPurchaseOrderItem.setActualExpiryDate(null);
        testPurchaseOrderItem.setNotes("Item notes");

        testPurchaseOrder.setItems(List.of(testPurchaseOrderItem));

        // Setup test create purchase order request
        CreatePurchaseOrderRequest.PurchaseOrderItemRequest itemRequest = 
            new CreatePurchaseOrderRequest.PurchaseOrderItemRequest();
        itemRequest.setMedicineId(1L);
        itemRequest.setRequestedQuantity(10);
        itemRequest.setUnitPrice(BigDecimal.valueOf(100.0));
        itemRequest.setExpectedExpiryDate(testDate.plusMonths(6));
        itemRequest.setNotes("Item notes");

        testCreatePurchaseOrderRequest = new CreatePurchaseOrderRequest();
        testCreatePurchaseOrderRequest.setSupplierId(1L);
        testCreatePurchaseOrderRequest.setWarehouseId(1L);
        testCreatePurchaseOrderRequest.setExpectedDeliveryDate(testDate);
        testCreatePurchaseOrderRequest.setNotes("Test notes");
        testCreatePurchaseOrderRequest.setItems(List.of(itemRequest));
    }

    // ==========================================
    // 1. BLACK-BOX TESTING (EP, BVA)
    // ==========================================

    /**
     * Kiểm thử Hộp đen: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
     * Target Hàm: getAllPurchaseOrders()
     * Phân tích kết quả trả về từ repository:
     * - Empty list -> trả về empty list
     * - Single item list -> trả về single item list  
     * - Multiple items list -> trả về multiple items list
     */
    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra getAllPurchaseOrders() với các trường hợp khác nhau")
    public void testGetAllPurchaseOrders_EquivalencePartition_BoundaryValue() {
        // Case 1: Empty list - Phân vùng rỗng
        when(purchaseOrderRepository.findAll()).thenReturn(new ArrayList<>());
        List<PurchaseOrderResponse> result1 = purchaseOrderService.getAllPurchaseOrders();
        assertTrue(result1.isEmpty());
        verify(purchaseOrderRepository, times(1)).findAll();

        // Case 2: Single item - Giá trị biên dưới
        when(purchaseOrderRepository.findAll()).thenReturn(List.of(testPurchaseOrder));
        List<PurchaseOrderResponse> result2 = purchaseOrderService.getAllPurchaseOrders();
        assertEquals(1, result2.size());
        assertEquals(1L, result2.get(0).getPurchaseOrderId());
        verify(purchaseOrderRepository, times(2)).findAll();

        // Case 3: Multiple items - Phân vùng bình thường
        PurchaseOrder order2 = new PurchaseOrder();
        order2.setPurchaseOrderId(2L);
        order2.setOrderCode("PO-2023-0002");
        order2.setSupplier(testSupplier);
        order2.setWarehouse(testWarehouse);
        order2.setCreatedBy(testUser);
        order2.setStatus(PurchaseOrderStatus.CONFIRMED);
        order2.setTotalAmount(BigDecimal.valueOf(500.0));
        
        when(purchaseOrderRepository.findAll()).thenReturn(List.of(testPurchaseOrder, order2));
        List<PurchaseOrderResponse> result3 = purchaseOrderService.getAllPurchaseOrders();
        assertEquals(2, result3.size());
        assertEquals(1L, result3.get(0).getPurchaseOrderId());
        assertEquals(2L, result3.get(1).getPurchaseOrderId());
        verify(purchaseOrderRepository, times(3)).findAll();
    }

    /**
     * Kiểm thử Hộp đen: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
     * Target Hàm: getPurchaseOrdersByStatus(String status)
     * Phân tích status parameter:
     * - Valid status -> trả về list orders
     * - Invalid status -> IllegalArgumentException
     */
    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra getPurchaseOrdersByStatus() với các status khác nhau")
    public void testGetPurchaseOrdersByStatus_EquivalencePartition_BoundaryValue() {
        // Case 1: Valid status with orders
        when(purchaseOrderRepository.findByStatus(PurchaseOrderStatus.PENDING))
            .thenReturn(List.of(testPurchaseOrder));
        List<PurchaseOrderResponse> result1 = purchaseOrderService.getPurchaseOrdersByStatus("PENDING");
        assertEquals(1, result1.size());
        assertEquals("PENDING", result1.get(0).getStatus());
        verify(purchaseOrderRepository, times(1)).findByStatus(PurchaseOrderStatus.PENDING);

        // Case 2: Valid status with no orders
        when(purchaseOrderRepository.findByStatus(PurchaseOrderStatus.CANCELLED))
            .thenReturn(new ArrayList<>());
        List<PurchaseOrderResponse> result2 = purchaseOrderService.getPurchaseOrdersByStatus("CANCELLED");
        assertTrue(result2.isEmpty());
        verify(purchaseOrderRepository, times(1)).findByStatus(PurchaseOrderStatus.CANCELLED);

        // Case 3: Invalid status
        assertThrows(IllegalArgumentException.class, () -> {
            purchaseOrderService.getPurchaseOrdersByStatus("INVALID_STATUS");
        });
    }

    /**
     * Kiểm thử Hộp đen: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
     * Target Hàm: getPurchaseOrdersByWarehouse(Long warehouseId)
     * Phân tích warehouseId parameter:
     * - Valid warehouseId -> trả về list orders
     * - Invalid warehouseId -> empty list
     * - Null warehouseId -> empty list
     */
    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra getPurchaseOrdersByWarehouse() với các warehouseId khác nhau")
    public void testGetPurchaseOrdersByWarehouse_EquivalencePartition_BoundaryValue() {
        // Case 1: Valid warehouseId with orders
        when(purchaseOrderRepository.findByWarehouse_WarehouseId(1L))
            .thenReturn(List.of(testPurchaseOrder));
        List<PurchaseOrderResponse> result1 = purchaseOrderService.getPurchaseOrdersByWarehouse(1L);
        assertEquals(1, result1.size());
        assertEquals(1L, result1.get(0).getWarehouse().getWarehouseId());
        verify(purchaseOrderRepository, times(1)).findByWarehouse_WarehouseId(1L);

        // Case 2: Valid warehouseId with no orders
        when(purchaseOrderRepository.findByWarehouse_WarehouseId(999L))
            .thenReturn(new ArrayList<>());
        List<PurchaseOrderResponse> result2 = purchaseOrderService.getPurchaseOrdersByWarehouse(999L);
        assertTrue(result2.isEmpty());
        verify(purchaseOrderRepository, times(1)).findByWarehouse_WarehouseId(999L);

        // Case 3: Null warehouseId
        when(purchaseOrderRepository.findByWarehouse_WarehouseId(null))
            .thenReturn(new ArrayList<>());
        List<PurchaseOrderResponse> result3 = purchaseOrderService.getPurchaseOrdersByWarehouse(null);
        assertTrue(result3.isEmpty());
        verify(purchaseOrderRepository, times(1)).findByWarehouse_WarehouseId(null);
    }

    // ==========================================
    // 2. WHITE-BOX TESTING (CFG, DFG)
    // ==========================================

    /**
     * Kiểm thử luồng điều khiển (CFG - Branch Coverage)
     * Dành cho hàm getPurchaseOrderById(Long id):
     * - Branch 1: purchaseOrderRepository.findByIdWithItems() trả về non-null -> tiếp tục xử lý
     * - Branch 2: purchaseOrderRepository.findByIdWithItems() trả về null -> ném RuntimeException
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getPurchaseOrderById() - Success path")
    public void testGetPurchaseOrderById_SuccessPath_BranchCoverage() {
        when(purchaseOrderRepository.findByIdWithItems(1L)).thenReturn(testPurchaseOrder);

        PurchaseOrderResponse result = purchaseOrderService.getPurchaseOrderById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getPurchaseOrderId());
        assertEquals("PO-2023-0001", result.getOrderCode());
        assertEquals("PENDING", result.getStatus());
        assertEquals(BigDecimal.valueOf(1000.0), result.getTotalAmount());
        assertEquals(testDate, result.getExpectedDeliveryDate());
        assertEquals("Test notes", result.getNotes());
        assertNotNull(result.getSupplier());
        assertNotNull(result.getWarehouse());
        assertNotNull(result.getCreatedBy());
        assertEquals(1, result.getItems().size());

        verify(purchaseOrderRepository, times(1)).findByIdWithItems(1L);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getPurchaseOrderById() - Exception path")
    public void testGetPurchaseOrderById_NotFound_BranchCoverage() {
        when(purchaseOrderRepository.findByIdWithItems(999L)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            purchaseOrderService.getPurchaseOrderById(999L);
        });

        assertEquals("Purchase order not found with id: 999", exception.getMessage());
        verify(purchaseOrderRepository, times(1)).findByIdWithItems(999L);
    }

    /**
     * Phân tích luồng dữ liệu (DFG - Data Flow Graph)
     * Hàm: createPurchaseOrder(CreatePurchaseOrderRequest request, Long userId)
     * Quy trình DFG:
     * 1. Define parameters `request`, `userId` -> Use trong validation
     * 2. Use request.getSupplierId() -> repository.findById() -> Define supplier
     * 3. Use request.getWarehouseId() -> repository.findById() -> Define warehouse
     * 4. Use userId -> repository.findById() -> Define user
     * 5. Use request.getItems() -> loop qua items -> Create order items
     * 6. Use order -> repository.save() -> Define savedOrder
     * 7. Use savedOrder -> convertToResponse() -> return result
     */
    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng dữ liệu createPurchaseOrder() đầy đủ")
    public void testCreatePurchaseOrder_CompleteDataFlow() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> {
            PurchaseOrder saved = invocation.getArgument(0);
            if (saved.getPurchaseOrderId() == null) {
                saved.setPurchaseOrderId(1L);
                saved.setOrderCode("PO-2023-0001");
                saved.setCreatedAt(LocalDateTime.now());
                saved.setUpdatedAt(LocalDateTime.now());
            }
            return saved;
        });

        PurchaseOrderResponse result = purchaseOrderService.createPurchaseOrder(testCreatePurchaseOrderRequest, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getPurchaseOrderId());
        assertEquals("PO-2023-0001", result.getOrderCode());
        assertEquals("PENDING", result.getStatus());
        assertEquals(BigDecimal.valueOf(1000.0), result.getTotalAmount());
        assertEquals(testDate, result.getExpectedDeliveryDate());
        assertEquals("Test notes", result.getNotes());
        assertNotNull(result.getSupplier());
        assertNotNull(result.getWarehouse());
        assertNotNull(result.getCreatedBy());
        assertEquals(1, result.getItems().size());

        verify(supplierRepository, times(1)).findById(1L);
        verify(warehouseRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(1L);
        verify(medicineRepository, times(1)).findById(1L);
        verify(purchaseOrderRepository, times(1)).save(any(PurchaseOrder.class));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createPurchaseOrder() - Supplier not found")
    public void testCreatePurchaseOrder_SupplierNotFound_BranchCoverage() {
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest();
        request.setSupplierId(999L);
        request.setItems(new ArrayList<>());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            purchaseOrderService.createPurchaseOrder(request, 1L);
        });

        assertEquals("Supplier not found", exception.getMessage());
        verify(supplierRepository, times(1)).findById(999L);
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createPurchaseOrder() - Warehouse not found")
    public void testCreatePurchaseOrder_WarehouseNotFound_BranchCoverage() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());

        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest();
        request.setSupplierId(1L);
        request.setWarehouseId(999L);
        request.setItems(new ArrayList<>());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            purchaseOrderService.createPurchaseOrder(request, 1L);
        });

        assertEquals("Warehouse not found", exception.getMessage());
        verify(supplierRepository, times(1)).findById(1L);
        verify(warehouseRepository, times(1)).findById(999L);
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createPurchaseOrder() - User not found")
    public void testCreatePurchaseOrder_UserNotFound_BranchCoverage() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            purchaseOrderService.createPurchaseOrder(testCreatePurchaseOrderRequest, 999L);
        });

        assertEquals("User not found", exception.getMessage());
        verify(supplierRepository, times(1)).findById(1L);
        verify(warehouseRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(999L);
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createPurchaseOrder() - Medicine not found")
    public void testCreatePurchaseOrder_MedicineNotFound_BranchCoverage() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(medicineRepository.findById(999L)).thenReturn(Optional.empty());

        CreatePurchaseOrderRequest.PurchaseOrderItemRequest itemRequest = 
            new CreatePurchaseOrderRequest.PurchaseOrderItemRequest();
        itemRequest.setMedicineId(999L);
        itemRequest.setRequestedQuantity(10);
        itemRequest.setUnitPrice(BigDecimal.valueOf(100.0));

        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest();
        request.setSupplierId(1L);
        request.setWarehouseId(1L);
        request.setItems(List.of(itemRequest));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            purchaseOrderService.createPurchaseOrder(request, 1L);
        });

        assertEquals("Medicine not found with id: 999", exception.getMessage());
        verify(supplierRepository, times(1)).findById(1L);
        verify(warehouseRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(1L);
        verify(medicineRepository, times(1)).findById(999L);
        verify(purchaseOrderRepository, never()).save(any());
    }

    /**
     * Kiểm thử luồng điều khiển cho confirmPurchaseOrder()
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh confirmPurchaseOrder() - Success path")
    public void testConfirmPurchaseOrder_Success_BranchCoverage() {
        testPurchaseOrder.setStatus(PurchaseOrderStatus.PENDING);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(testPurchaseOrder));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrderResponse result = purchaseOrderService.confirmPurchaseOrder(1L);

        assertNotNull(result);
        assertEquals("CONFIRMED", result.getStatus());

        verify(purchaseOrderRepository, times(1)).findById(1L);
        verify(purchaseOrderRepository, times(1)).save(testPurchaseOrder);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh confirmPurchaseOrder() - Order not found")
    public void testConfirmPurchaseOrder_NotFound_BranchCoverage() {
        when(purchaseOrderRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            purchaseOrderService.confirmPurchaseOrder(999L);
        });

        assertEquals("Purchase order not found", exception.getMessage());
        verify(purchaseOrderRepository, times(1)).findById(999L);
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh confirmPurchaseOrder() - Invalid status")
    public void testConfirmPurchaseOrder_InvalidStatus_BranchCoverage() {
        testPurchaseOrder.setStatus(PurchaseOrderStatus.CONFIRMED);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(testPurchaseOrder));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            purchaseOrderService.confirmPurchaseOrder(1L);
        });

        assertEquals("Order must be in PENDING status to confirm", exception.getMessage());
        verify(purchaseOrderRepository, times(1)).findById(1L);
        verify(purchaseOrderRepository, never()).save(any());
    }

    /**
     * Kiểm thử luồng điều khiển cho updateOrderStatus()
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh updateOrderStatus() - Success path")
    public void testUpdateOrderStatus_Success_BranchCoverage() {
        testPurchaseOrder.setStatus(PurchaseOrderStatus.CONFIRMED);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(testPurchaseOrder));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrderResponse result = purchaseOrderService.updateOrderStatus(1L, "SHIPPING");

        assertNotNull(result);
        assertEquals("SHIPPING", result.getStatus());

        verify(purchaseOrderRepository, times(1)).findById(1L);
        verify(purchaseOrderRepository, times(1)).save(testPurchaseOrder);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh updateOrderStatus() - Order not found")
    public void testUpdateOrderStatus_NotFound_BranchCoverage() {
        when(purchaseOrderRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            purchaseOrderService.updateOrderStatus(999L, "CONFIRMED");
        });

        assertEquals("Purchase order not found", exception.getMessage());
        verify(purchaseOrderRepository, times(1)).findById(999L);
        verify(purchaseOrderRepository, never()).save(any());
    }

    /**
     * Kiểm thử luồng điều khiển cho cancelPurchaseOrder()
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh cancelPurchaseOrder() - Success path")
    public void testCancelPurchaseOrder_Success_BranchCoverage() {
        testPurchaseOrder.setStatus(PurchaseOrderStatus.PENDING);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(testPurchaseOrder));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        purchaseOrderService.cancelPurchaseOrder(1L);

        assertEquals(PurchaseOrderStatus.CANCELLED, testPurchaseOrder.getStatus());

        verify(purchaseOrderRepository, times(1)).findById(1L);
        verify(purchaseOrderRepository, times(1)).save(testPurchaseOrder);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh cancelPurchaseOrder() - Order not found")
    public void testCancelPurchaseOrder_NotFound_BranchCoverage() {
        when(purchaseOrderRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            purchaseOrderService.cancelPurchaseOrder(999L);
        });

        assertEquals("Purchase order not found", exception.getMessage());
        verify(purchaseOrderRepository, times(1)).findById(999L);
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh cancelPurchaseOrder() - Cannot cancel received order")
    public void testCancelPurchaseOrder_CannotCancelReceived_BranchCoverage() {
        testPurchaseOrder.setStatus(PurchaseOrderStatus.RECEIVED);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(testPurchaseOrder));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            purchaseOrderService.cancelPurchaseOrder(1L);
        });

        assertEquals("Cannot cancel order that has been received or approved", exception.getMessage());
        verify(purchaseOrderRepository, times(1)).findById(1L);
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh cancelPurchaseOrder() - Cannot cancel approved order")
    public void testCancelPurchaseOrder_CannotCancelApproved_BranchCoverage() {
        testPurchaseOrder.setStatus(PurchaseOrderStatus.APPROVED);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(testPurchaseOrder));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            purchaseOrderService.cancelPurchaseOrder(1L);
        });

        assertEquals("Cannot cancel order that has been received or approved", exception.getMessage());
        verify(purchaseOrderRepository, times(1)).findById(1L);
        verify(purchaseOrderRepository, never()).save(any());
    }

    // ==========================================
    // 3. PRIVATE METHOD TESTING
    // ==========================================

    /**
     * Test private method convertToResponse() thông qua các public methods
     * Verify field mapping và null handling
     */
    @Test
    @DisplayName("Private Method | convertToResponse: Test complete field mapping")
    public void testConvertToResponse_CompleteFieldMapping() {
        when(purchaseOrderRepository.findByIdWithItems(1L)).thenReturn(testPurchaseOrder);

        PurchaseOrderResponse result = purchaseOrderService.getPurchaseOrderById(1L);

        assertNotNull(result);
        assertEquals(testPurchaseOrder.getPurchaseOrderId(), result.getPurchaseOrderId());
        assertEquals(testPurchaseOrder.getOrderCode(), result.getOrderCode());
        assertEquals(testPurchaseOrder.getStatus().name(), result.getStatus());
        assertEquals(testPurchaseOrder.getTotalAmount(), result.getTotalAmount());
        assertEquals(testPurchaseOrder.getExpectedDeliveryDate(), result.getExpectedDeliveryDate());
        assertEquals(testPurchaseOrder.getNotes(), result.getNotes());
        assertEquals(testPurchaseOrder.getCreatedAt(), result.getCreatedAt());
        assertEquals(testPurchaseOrder.getUpdatedAt(), result.getUpdatedAt());

        // Verify nested objects
        assertNotNull(result.getSupplier());
        assertEquals(testSupplier.getSupplierId(), result.getSupplier().getSupplierId());
        assertEquals(testSupplier.getSupplierName(), result.getSupplier().getSupplierName());

        assertNotNull(result.getWarehouse());
        assertEquals(testWarehouse.getWarehouseId(), result.getWarehouse().getWarehouseId());
        assertEquals(testWarehouse.getName(), result.getWarehouse().getWarehouseName());

        assertNotNull(result.getCreatedBy());
        assertEquals(testUser.getUserId(), result.getCreatedBy().getUserId());
        assertEquals(testUser.getUsername(), result.getCreatedBy().getUsername());
        assertEquals(testUser.getFullName(), result.getCreatedBy().getFullName());

        // Verify items
        assertNotNull(result.getItems());
        assertEquals(1, result.getItems().size());
        PurchaseOrderResponse.PurchaseOrderItemResponse itemResponse = result.getItems().get(0);
        assertEquals(testPurchaseOrderItem.getItemId(), itemResponse.getItemId());
        assertEquals(testPurchaseOrderItem.getRequestedQuantity(), itemResponse.getRequestedQuantity());
        assertEquals(testPurchaseOrderItem.getUnitPrice(), itemResponse.getUnitPrice());
        assertEquals(testPurchaseOrderItem.getTotalPrice(), itemResponse.getTotalPrice());

        verify(purchaseOrderRepository, times(1)).findByIdWithItems(1L);
    }

    /**
     * Test private method convertItemToResponse() thông qua các public methods
     * Verify field mapping và null handling
     */
    @Test
    @DisplayName("Private Method | convertItemToResponse: Test complete field mapping")
    public void testConvertItemToResponse_CompleteFieldMapping() {
        when(purchaseOrderRepository.findByIdWithItems(1L)).thenReturn(testPurchaseOrder);

        PurchaseOrderResponse result = purchaseOrderService.getPurchaseOrderById(1L);
        PurchaseOrderResponse.PurchaseOrderItemResponse itemResponse = result.getItems().get(0);

        assertNotNull(itemResponse);
        assertEquals(testPurchaseOrderItem.getItemId(), itemResponse.getItemId());
        assertEquals(testPurchaseOrderItem.getRequestedQuantity(), itemResponse.getRequestedQuantity());
        assertEquals(testPurchaseOrderItem.getReceivedQuantity(), itemResponse.getReceivedQuantity());
        assertEquals(testPurchaseOrderItem.getUnitPrice(), itemResponse.getUnitPrice());
        assertEquals(testPurchaseOrderItem.getTotalPrice(), itemResponse.getTotalPrice());
        assertEquals(testPurchaseOrderItem.getExpectedExpiryDate(), itemResponse.getExpectedExpiryDate());
        assertEquals(testPurchaseOrderItem.getActualExpiryDate(), itemResponse.getActualExpiryDate());
        assertEquals(testPurchaseOrderItem.getNotes(), itemResponse.getNotes());

        // Verify medicine info
        assertNotNull(itemResponse.getMedicine());
        assertEquals(testMedicine.getMedicineId(), itemResponse.getMedicine().getMedicineId());
        assertEquals(testMedicine.getName(), itemResponse.getMedicine().getMedicineName());
        assertEquals(testMedicine.getManufacturer(), itemResponse.getMedicine().getSku());

        verify(purchaseOrderRepository, times(1)).findByIdWithItems(1L);
    }

    /**
     * Test private method convertSupplierToResponse() thông qua các public methods
     * Verify field mapping
     */
    @Test
    @DisplayName("Private Method | convertSupplierToResponse: Test complete field mapping")
    public void testConvertSupplierToResponse_CompleteFieldMapping() {
        when(purchaseOrderRepository.findByIdWithItems(1L)).thenReturn(testPurchaseOrder);

        PurchaseOrderResponse result = purchaseOrderService.getPurchaseOrderById(1L);
        SupplierResponse supplierResponse = result.getSupplier();

        assertNotNull(supplierResponse);
        assertEquals(testSupplier.getSupplierId(), supplierResponse.getSupplierId());
        assertEquals(testSupplier.getSupplierName(), supplierResponse.getSupplierName());
        assertEquals(testSupplier.getContactPerson(), supplierResponse.getContactPerson());
        assertEquals(testSupplier.getPhoneNumber(), supplierResponse.getPhoneNumber());
        assertEquals(testSupplier.getEmail(), supplierResponse.getEmail());
        assertEquals(testSupplier.getAddress(), supplierResponse.getAddress());
        assertEquals(testSupplier.getTaxCode(), supplierResponse.getTaxCode());
        assertEquals(testSupplier.getStatus().name(), supplierResponse.getStatus());
        assertEquals(testSupplier.getCreatedAt(), supplierResponse.getCreatedAt());
        assertEquals(testSupplier.getUpdatedAt(), supplierResponse.getUpdatedAt());

        verify(purchaseOrderRepository, times(1)).findByIdWithItems(1L);
    }

    // ==========================================
    // 4. SUPPLEMENTARY COVERAGE TESTS
    // ==========================================

    @Test
    @DisplayName("Supplementary: Test createPurchaseOrder() with empty items list")
    public void testCreatePurchaseOrder_EmptyItemsList() {
        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest();
        request.setSupplierId(1L);
        request.setWarehouseId(1L);
        request.setItems(new ArrayList<>()); // Empty items

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            purchaseOrderService.createPurchaseOrder(request, 1L);
        });
        assertEquals("Purchase order must include at least one item", exception.getMessage());

        verify(supplierRepository, times(1)).findById(1L);
        verify(warehouseRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(1L);
        verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
    }

    @Test
    @DisplayName("Supplementary: Test createPurchaseOrder() with multiple items")
    public void testCreatePurchaseOrder_MultipleItems() {
        Medicine medicine2 = new Medicine();
        medicine2.setMedicineId(2L);
        medicine2.setName("Test Medicine 2");
        medicine2.setManufacturer("Test Manufacturer 2");

        CreatePurchaseOrderRequest.PurchaseOrderItemRequest itemRequest2 = 
            new CreatePurchaseOrderRequest.PurchaseOrderItemRequest();
        itemRequest2.setMedicineId(2L);
        itemRequest2.setRequestedQuantity(5);
        itemRequest2.setUnitPrice(BigDecimal.valueOf(50.0));

        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest();
        request.setSupplierId(1L);
        request.setWarehouseId(1L);
        request.setItems(List.of(
            testCreatePurchaseOrderRequest.getItems().get(0),
            itemRequest2
        ));

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));
        when(medicineRepository.findById(2L)).thenReturn(Optional.of(medicine2));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> {
            PurchaseOrder saved = invocation.getArgument(0);
            if (saved.getPurchaseOrderId() == null) {
                saved.setPurchaseOrderId(1L);
                saved.setOrderCode("PO-2023-0001");
                saved.setCreatedAt(LocalDateTime.now());
                saved.setUpdatedAt(LocalDateTime.now());
            }
            return saved;
        });

        PurchaseOrderResponse result = purchaseOrderService.createPurchaseOrder(request, 1L);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(1250.0), result.getTotalAmount()); // (10*100) + (5*50)
        assertEquals(2, result.getItems().size());

        verify(supplierRepository, times(1)).findById(1L);
        verify(warehouseRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(1L);
        verify(medicineRepository, times(1)).findById(1L);
        verify(medicineRepository, times(1)).findById(2L);
        verify(purchaseOrderRepository, times(1)).save(any(PurchaseOrder.class));
    }

    @Test
    @DisplayName("Supplementary: Test getAllPurchaseOrders repository exception")
    public void testGetAllPurchaseOrdersRepositoryException() {
        when(purchaseOrderRepository.findAll()).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            purchaseOrderService.getAllPurchaseOrders();
        });
    }

    @Test
    @DisplayName("Supplementary: Test getPurchaseOrderById repository exception")
    public void testGetPurchaseOrderByIdRepositoryException() {
        when(purchaseOrderRepository.findByIdWithItems(1L)).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            purchaseOrderService.getPurchaseOrderById(1L);
        });
    }

    @Test
    @DisplayName("Supplementary: Test getPurchaseOrdersByStatus invalid status")
    public void testGetPurchaseOrdersByStatusInvalidStatus() {
        assertThrows(IllegalArgumentException.class, () -> {
            purchaseOrderService.getPurchaseOrdersByStatus("INVALID_STATUS");
        });
    }

    @Test
    @DisplayName("Supplementary: Test getPurchaseOrdersByWarehouse repository exception")
    public void testGetPurchaseOrdersByWarehouseRepositoryException() {
        when(purchaseOrderRepository.findByWarehouse_WarehouseId(1L)).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            purchaseOrderService.getPurchaseOrdersByWarehouse(1L);
        });
    }

    @Test
    @DisplayName("Supplementary: Test createPurchaseOrder repository exception")
    public void testCreatePurchaseOrderRepositoryException() {
        when(supplierRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            purchaseOrderService.createPurchaseOrder(testCreatePurchaseOrderRequest, 1L);
        });
    }

    @Test
    @DisplayName("Supplementary: Test confirmPurchaseOrder repository exception")
    public void testConfirmPurchaseOrderRepositoryException() {
        when(purchaseOrderRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            purchaseOrderService.confirmPurchaseOrder(1L);
        });
    }

    @Test
    @DisplayName("Supplementary: Test updateOrderStatus repository exception")
    public void testUpdateOrderStatusRepositoryException() {
        when(purchaseOrderRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            purchaseOrderService.updateOrderStatus(1L, "CONFIRMED");
        });
    }

    @Test
    @DisplayName("Supplementary: Test cancelPurchaseOrder repository exception")
    public void testCancelPurchaseOrderRepositoryException() {
        when(purchaseOrderRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            purchaseOrderService.cancelPurchaseOrder(1L);
        });
    }

    @Test
    @DisplayName("Supplementary: Test createPurchaseOrder() save exception")
    public void testCreatePurchaseOrder_SaveException() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenThrow(new RuntimeException("Save failed"));

        assertThrows(RuntimeException.class, () -> {
            purchaseOrderService.createPurchaseOrder(testCreatePurchaseOrderRequest, 1L);
        });

        verify(supplierRepository, times(1)).findById(1L);
        verify(warehouseRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(1L);
        verify(medicineRepository, times(1)).findById(1L);
        verify(purchaseOrderRepository, times(1)).save(any(PurchaseOrder.class));
    }

    @Test
    @DisplayName("Supplementary: Test updateOrderStatus() save exception")
    public void testUpdateOrderStatus_SaveException() {
        testPurchaseOrder.setStatus(PurchaseOrderStatus.CONFIRMED);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(testPurchaseOrder));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenThrow(new RuntimeException("Save failed"));

        assertThrows(RuntimeException.class, () -> {
            purchaseOrderService.updateOrderStatus(1L, "SHIPPING");
        });

        verify(purchaseOrderRepository, times(1)).findById(1L);
        verify(purchaseOrderRepository, times(1)).save(any(PurchaseOrder.class));
    }

    @Test
    @DisplayName("Supplementary: Test cancelPurchaseOrder() save exception")
    public void testCancelPurchaseOrder_SaveException() {
        testPurchaseOrder.setStatus(PurchaseOrderStatus.PENDING);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(testPurchaseOrder));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenThrow(new RuntimeException("Save failed"));

        assertThrows(RuntimeException.class, () -> {
            purchaseOrderService.cancelPurchaseOrder(1L);
        });

        verify(purchaseOrderRepository, times(1)).findById(1L);
        verify(purchaseOrderRepository, times(1)).save(any(PurchaseOrder.class));
    }

    @Test
    @DisplayName("Supplementary: Test createPurchaseOrder() with null optional fields")
    public void testCreatePurchaseOrder_NullOptionalFields() {
        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest();
        request.setSupplierId(1L);
        request.setWarehouseId(1L);
        request.setExpectedDeliveryDate(null); // Null date
        request.setNotes(null); // Null notes
        request.setItems(testCreatePurchaseOrderRequest.getItems());

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> {
            PurchaseOrder saved = invocation.getArgument(0);
            if (saved.getPurchaseOrderId() == null) {
                saved.setPurchaseOrderId(1L);
                saved.setOrderCode("PO-2023-0001");
                saved.setCreatedAt(LocalDateTime.now());
                saved.setUpdatedAt(LocalDateTime.now());
            }
            return saved;
        });

        PurchaseOrderResponse result = purchaseOrderService.createPurchaseOrder(request, 1L);

        assertNotNull(result);
        assertNull(result.getExpectedDeliveryDate());
        assertNull(result.getNotes());

        verify(supplierRepository, times(1)).findById(1L);
        verify(warehouseRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(1L);
        verify(medicineRepository, times(1)).findById(1L);
        verify(purchaseOrderRepository, times(1)).save(any(PurchaseOrder.class));
    }
}
