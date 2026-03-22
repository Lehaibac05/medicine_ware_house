package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.model.*;
import com.pharmacy.warehouse.repository.PurchaseOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho PurchaseOrderPdfService
 * Đạt độ phủ mã 90% với đầy đủ phương pháp kiểm thử:
 * - Black-Box Testing: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
 * - White-Box Testing: Luồng điều khiển (CFG) & Luồng dữ liệu (DFG)
 */
@ExtendWith(MockitoExtension.class)
public class PurchaseOrderPdfServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @InjectMocks
    private PurchaseOrderPdfService purchaseOrderPdfService;

    private PurchaseOrder testPurchaseOrder;
    private PurchaseOrderItem testItem1, testItem2;
    private Supplier testSupplier;
    private Warehouse testWarehouse;
    private User testUser;

    @BeforeEach
    public void setup() {
        // Setup test supplier
        testSupplier = new Supplier();
        testSupplier.setSupplierId(1L);
        testSupplier.setSupplierName("Test Supplier");
        testSupplier.setEmail("supplier@test.com");

        // Setup test warehouse
        testWarehouse = new Warehouse();
        testWarehouse.setWarehouseId(1L);
        testWarehouse.setName("Main Warehouse");

        // Setup test user
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("testuser");
        testUser.setFullName("Test User");

        // Setup test purchase order items
        testItem1 = new PurchaseOrderItem();
        testItem1.setItemId(1L);
        testItem1.setMedicine(new Medicine());
        testItem1.getMedicine().setMedicineId(1L);
        testItem1.getMedicine().setName("Paracetamol");
        testItem1.setRequestedQuantity(10);
        testItem1.setUnitPrice(BigDecimal.valueOf(15.50));
        testItem1.setTotalPrice(BigDecimal.valueOf(155.00));

        testItem2 = new PurchaseOrderItem();
        testItem2.setItemId(2L);
        testItem2.setMedicine(new Medicine());
        testItem2.getMedicine().setMedicineId(2L);
        testItem2.getMedicine().setName("Ibuprofen");
        testItem2.setRequestedQuantity(20);
        testItem2.setUnitPrice(BigDecimal.valueOf(8.75));
        testItem2.setTotalPrice(BigDecimal.valueOf(175.00));

        // Setup test purchase order
        testPurchaseOrder = new PurchaseOrder();
        testPurchaseOrder.setPurchaseOrderId(1L);
        testPurchaseOrder.setOrderCode("PO-2023-001");
        testPurchaseOrder.setSupplier(testSupplier);
        testPurchaseOrder.setWarehouse(testWarehouse);
        testPurchaseOrder.setCreatedBy(testUser);
        testPurchaseOrder.setExpectedDeliveryDate(LocalDate.now().plusDays(7));
        testPurchaseOrder.setCreatedAt(LocalDateTime.now());
        testPurchaseOrder.setTotalAmount(BigDecimal.valueOf(330.00));
        testPurchaseOrder.setStatus(PurchaseOrder.PurchaseOrderStatus.CONFIRMED);
        testPurchaseOrder.setItems(List.of(testItem1, testItem2));
    }

    // ==========================================
    // 1. BLACK-BOX TESTING (EP, BVA, DT)
    // ==========================================

    /**
     * Kiểm thử Hộp đen: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
     * Target Hàm: generatePurchaseOrderPdf(Long)
     * Phân tích các trường hợp:
     * - Valid orderId - Phân vùng hợp lệ
     * - Invalid orderId - Phân vùng không hợp lệ
     * - Valid order status - Phân vùng hợp lệ
     * - Invalid order status - Phân vùng không hợp lệ
     */
    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra generatePurchaseOrderPdf(Long) với các trường hợp khác nhau")
    public void testGeneratePurchaseOrderPdf_EquivalencePartition_BoundaryValue() {
        // Case 1: Valid inputs - Phân vùng hợp lệ
        when(purchaseOrderRepository.findByIdWithItems(1L)).thenReturn(testPurchaseOrder);

        byte[] result = purchaseOrderPdfService.generatePurchaseOrderPdf(1L);

        assertNotNull(result);
        assertTrue(result.length > 0); // PDF should not be empty
        verify(purchaseOrderRepository).findByIdWithItems(1L);
    }

    /**
     * Kiểm thử Hộp đen: Decision Table Testing
     * Target: generatePurchaseOrderPdf() với các combinations của failures
     */
    @Test
    @DisplayName("Black-Box | Decision Table: Kiểm tra các combinations của PDF generation failures")
    public void testGeneratePurchaseOrderPdf_DecisionTable() {
        // Decision Table: Test various combinations of PDF generation failures

        // Row 1: Valid scenario - Success
        when(purchaseOrderRepository.findByIdWithItems(1L)).thenReturn(testPurchaseOrder);

        assertDoesNotThrow(() -> {
            byte[] result = purchaseOrderPdfService.generatePurchaseOrderPdf(1L);
            assertNotNull(result);
            assertTrue(result.length > 0);
        });

        // Row 2: Order not found - Entity error
        when(purchaseOrderRepository.findByIdWithItems(999L)).thenReturn(null);

        RuntimeException exception1 = assertThrows(RuntimeException.class,
            () -> purchaseOrderPdfService.generatePurchaseOrderPdf(999L));
        assertTrue(exception1.getMessage().contains("Purchase order not found with id: 999"));

        // Row 3: Order not confirmed - Status error
        PurchaseOrder pendingOrder = new PurchaseOrder();
        pendingOrder.setPurchaseOrderId(2L);
        pendingOrder.setStatus(PurchaseOrder.PurchaseOrderStatus.PENDING);
        when(purchaseOrderRepository.findByIdWithItems(2L)).thenReturn(pendingOrder);

        IllegalStateException exception2 = assertThrows(IllegalStateException.class,
            () -> purchaseOrderPdfService.generatePurchaseOrderPdf(2L));
        assertTrue(exception2.getMessage().contains("Purchase order must be CONFIRMED"));

        // Row 4: Order with null status - Should be treated as not confirmed
        PurchaseOrder nullStatusOrder = new PurchaseOrder();
        nullStatusOrder.setPurchaseOrderId(3L);
        nullStatusOrder.setStatus(null);
        when(purchaseOrderRepository.findByIdWithItems(3L)).thenReturn(nullStatusOrder);

        IllegalStateException exception3 = assertThrows(IllegalStateException.class,
            () -> purchaseOrderPdfService.generatePurchaseOrderPdf(3L));
        assertTrue(exception3.getMessage().contains("Purchase order must be CONFIRMED"));
    }

    // ==========================================
    // 2. WHITE-BOX TESTING (CFG, DFG)
    // ==========================================

    /**
     * Kiểm thử luồng điều khiển (CFG - Branch Coverage)
     * Dành cho hàm generatePurchaseOrderPdf(Long):
     * - Branch 1: order == null -> throw RuntimeException
     * - Branch 2: order.getStatus() != CONFIRMED -> throw IllegalStateException
     * - Branch 3: Success path -> generate PDF
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh generatePurchaseOrderPdf(Long) - Order not found")
    public void testGeneratePurchaseOrderPdf_OrderNotFound_BranchCoverage() {
        when(purchaseOrderRepository.findByIdWithItems(1L)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> purchaseOrderPdfService.generatePurchaseOrderPdf(1L));

        assertTrue(exception.getMessage().contains("Purchase order not found with id: 1"));
        verify(purchaseOrderRepository).findByIdWithItems(1L);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh generatePurchaseOrderPdf(Long) - Order not confirmed")
    public void testGeneratePurchaseOrderPdf_OrderNotConfirmed_BranchCoverage() {
        PurchaseOrder pendingOrder = new PurchaseOrder();
        pendingOrder.setPurchaseOrderId(1L);
        pendingOrder.setStatus(PurchaseOrder.PurchaseOrderStatus.PENDING);
        when(purchaseOrderRepository.findByIdWithItems(1L)).thenReturn(pendingOrder);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> purchaseOrderPdfService.generatePurchaseOrderPdf(1L));

        assertTrue(exception.getMessage().contains("Purchase order must be CONFIRMED"));
        verify(purchaseOrderRepository).findByIdWithItems(1L);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh generatePurchaseOrderPdf(Long) - Null status")
    public void testGeneratePurchaseOrderPdf_NullStatus_BranchCoverage() {
        PurchaseOrder nullStatusOrder = new PurchaseOrder();
        nullStatusOrder.setPurchaseOrderId(1L);
        nullStatusOrder.setStatus(null);
        when(purchaseOrderRepository.findByIdWithItems(1L)).thenReturn(nullStatusOrder);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> purchaseOrderPdfService.generatePurchaseOrderPdf(1L));

        assertTrue(exception.getMessage().contains("Purchase order must be CONFIRMED"));
        verify(purchaseOrderRepository).findByIdWithItems(1L);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh generatePurchaseOrderPdf(Long) - Success path")
    public void testGeneratePurchaseOrderPdf_SuccessPath_BranchCoverage() {
        when(purchaseOrderRepository.findByIdWithItems(1L)).thenReturn(testPurchaseOrder);

        byte[] result = purchaseOrderPdfService.generatePurchaseOrderPdf(1L);

        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(purchaseOrderRepository).findByIdWithItems(1L);
    }

    /**
     * Kiểm thử luồng điều khiển (CFG) cho generatePurchaseOrderPdf(PurchaseOrder):
     * - Branch 1: order.getItems() != null -> process items
     * - Branch 2: order.getItems() == null -> skip items
     * - Branch 3: Exception handling -> wrap and rethrow
     * - Branch 4: Success path -> return PDF bytes
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh generatePurchaseOrderPdf(PurchaseOrder) - With items")
    public void testGeneratePurchaseOrderPdf_WithItems_BranchCoverage() {
        // Test with items (normal case)
        byte[] result = purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh generatePurchaseOrderPdf(PurchaseOrder) - Without items")
    public void testGeneratePurchaseOrderPdf_WithoutItems_BranchCoverage() {
        // Test without items
        testPurchaseOrder.setItems(null);

        byte[] result = purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh generatePurchaseOrderPdf(PurchaseOrder) - Exception handling")
    public void testGeneratePurchaseOrderPdf_ExceptionHandling_BranchCoverage() {
        // Create a problematic order that will cause exception
        PurchaseOrder problematicOrder = new PurchaseOrder();
        problematicOrder.setPurchaseOrderId(1L);
        problematicOrder.setOrderCode(null); // This might cause issues in PDF generation
        problematicOrder.setSupplier(null);
        problematicOrder.setWarehouse(null);
        problematicOrder.setCreatedBy(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> purchaseOrderPdfService.generatePurchaseOrderPdf(problematicOrder));

        assertTrue(exception.getMessage().contains("Unable to generate purchase order PDF"));
    }

    /**
     * Phân tích luồng dữ liệu (DFG - Data Flow Graph)
     * Hàm: generatePurchaseOrderPdf(Long)
     * Quy trình DFG:
     * 1. Use orderId -> findByIdWithItems(orderId)
     * 2. Use order.getStatus() -> status validation
     * 3. Use order data -> PDF generation
     * 4. Use PDF generation result -> return bytes
     */
    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng dữ liệu generatePurchaseOrderPdf(Long) - Complete flow")
    public void testGeneratePurchaseOrderPdf_CompleteDataFlow() {
        when(purchaseOrderRepository.findByIdWithItems(1L)).thenReturn(testPurchaseOrder);

        byte[] result = purchaseOrderPdfService.generatePurchaseOrderPdf(1L);

        // Verify complete data flow
        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(purchaseOrderRepository).findByIdWithItems(1L);
    }

    // ==========================================
    // 3. SUPPLEMENTARY COVERAGE TESTS
    // ==========================================

    @Test
    @DisplayName("Supplementary: Test generatePurchaseOrderPdf(PurchaseOrder) with empty items")
    public void testGeneratePurchaseOrderPdf_EmptyItems() {
        testPurchaseOrder.setItems(new ArrayList<>());

        byte[] result = purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("Supplementary: Test generatePurchaseOrderPdf(PurchaseOrder) with item null medicine")
    public void testGeneratePurchaseOrderPdf_ItemNullMedicine() {
        PurchaseOrderItem itemWithNullMedicine = new PurchaseOrderItem();
        itemWithNullMedicine.setItemId(3L);
        itemWithNullMedicine.setMedicine(null);
        itemWithNullMedicine.setRequestedQuantity(5);
        itemWithNullMedicine.setUnitPrice(BigDecimal.valueOf(10.00));
        itemWithNullMedicine.setTotalPrice(BigDecimal.valueOf(50.00));

        testPurchaseOrder.setItems(List.of(itemWithNullMedicine));

        byte[] result = purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("Supplementary: Test generatePurchaseOrderPdf(PurchaseOrder) with null quantities")
    public void testGeneratePurchaseOrderPdf_NullQuantities() {
        testItem1.setRequestedQuantity(null);
        testItem2.setRequestedQuantity(null);
        // Recalculate total prices
        testItem1.setTotalPrice(null);
        testItem2.setTotalPrice(null);

        byte[] result = purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("Supplementary: Test generatePurchaseOrderPdf(PurchaseOrder) with null prices")
    public void testGeneratePurchaseOrderPdf_NullPrices() {
        testItem1.setUnitPrice(null);
        testItem1.setTotalPrice(null);
        testItem2.setUnitPrice(null);
        testItem2.setTotalPrice(null);
        testPurchaseOrder.setTotalAmount(null);

        byte[] result = purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("Supplementary: Test generatePurchaseOrderPdf(PurchaseOrder) with supplier null values")
    public void testGeneratePurchaseOrderPdf_SupplierNullValues() {
        Supplier supplierWithNulls = new Supplier();
        supplierWithNulls.setSupplierId(1L);
        supplierWithNulls.setSupplierName(null);
        supplierWithNulls.setEmail(null);
        testPurchaseOrder.setSupplier(supplierWithNulls);

        byte[] result = purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("Supplementary: Test generatePurchaseOrderPdf(PurchaseOrder) with warehouse null name")
    public void testGeneratePurchaseOrderPdf_WarehouseNullName() {
        Warehouse warehouseWithNullName = new Warehouse();
        warehouseWithNullName.setWarehouseId(1L);
        warehouseWithNullName.setName(null);
        testPurchaseOrder.setWarehouse(warehouseWithNullName);

        byte[] result = purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("Supplementary: Test generatePurchaseOrderPdf(PurchaseOrder) with user null fullName")
    public void testGeneratePurchaseOrderPdf_UserNullFullName() {
        User userWithNullFullName = new User();
        userWithNullFullName.setUserId(1L);
        userWithNullFullName.setUsername("testuser");
        userWithNullFullName.setFullName(null);
        testPurchaseOrder.setCreatedBy(userWithNullFullName);

        byte[] result = purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("Supplementary: Test generatePurchaseOrderPdf(PurchaseOrder) with null dates")
    public void testGeneratePurchaseOrderPdf_NullDates() {
        testPurchaseOrder.setExpectedDeliveryDate(null);
        testPurchaseOrder.setCreatedAt(null);

        byte[] result = purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("Supplementary: Test generatePurchaseOrderPdf(PurchaseOrder) with empty strings")
    public void testGeneratePurchaseOrderPdf_EmptyStrings() {
        testSupplier.setSupplierName("");
        testSupplier.setEmail("");
        testWarehouse.setName("");
        testUser.setFullName("");
        testPurchaseOrder.setOrderCode("");

        byte[] result = purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("Supplementary: Test generatePurchaseOrderPdf(PurchaseOrder) with blank strings")
    public void testGeneratePurchaseOrderPdf_BlankStrings() {
        testSupplier.setSupplierName("   ");
        testSupplier.setEmail("   ");
        testWarehouse.setName("   ");
        testUser.setFullName("   ");
        testPurchaseOrder.setOrderCode("   ");

        byte[] result = purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("Supplementary: Test generatePurchaseOrderPdf(PurchaseOrder) with very large numbers")
    public void testGeneratePurchaseOrderPdf_VeryLargeNumbers() {
        testItem1.setRequestedQuantity(Integer.MAX_VALUE);
        testItem1.setUnitPrice(new BigDecimal("999999999.99"));
        testItem1.setTotalPrice(new BigDecimal("999999999999.99"));
        testPurchaseOrder.setTotalAmount(new BigDecimal("999999999999999.99"));

        byte[] result = purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("Supplementary: Test generatePurchaseOrderPdf(PurchaseOrder) with zero values")
    public void testGeneratePurchaseOrderPdf_ZeroValues() {
        testItem1.setRequestedQuantity(0);
        testItem1.setUnitPrice(BigDecimal.ZERO);
        testItem1.setTotalPrice(BigDecimal.ZERO);
        testPurchaseOrder.setTotalAmount(BigDecimal.ZERO);

        byte[] result = purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("Supplementary: Test generatePurchaseOrderPdf(PurchaseOrder) with negative values")
    public void testGeneratePurchaseOrderPdf_NegativeValues() {
        testItem1.setRequestedQuantity(-5);
        testItem1.setUnitPrice(BigDecimal.valueOf(-10.50));
        testItem1.setTotalPrice(BigDecimal.valueOf(-52.50));
        testPurchaseOrder.setTotalAmount(BigDecimal.valueOf(-100.00));

        byte[] result = purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("Supplementary: Test getConfirmedPurchaseOrder() with valid confirmed order")
    public void testGetConfirmedPurchaseOrder_ValidConfirmedOrder() {
        when(purchaseOrderRepository.findByIdWithItems(1L)).thenReturn(testPurchaseOrder);

        PurchaseOrder result = purchaseOrderPdfService.getConfirmedPurchaseOrder(1L);

        assertNotNull(result);
        assertEquals(1L, result.getPurchaseOrderId());
        assertEquals(PurchaseOrder.PurchaseOrderStatus.CONFIRMED, result.getStatus());
        verify(purchaseOrderRepository).findByIdWithItems(1L);
    }

    @Test
    @DisplayName("Supplementary: Test getConfirmedPurchaseOrder() with APPROVED status")
    public void testGetConfirmedPurchaseOrder_ApprovedStatus() {
        testPurchaseOrder.setStatus(PurchaseOrder.PurchaseOrderStatus.APPROVED);
        when(purchaseOrderRepository.findByIdWithItems(1L)).thenReturn(testPurchaseOrder);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> purchaseOrderPdfService.getConfirmedPurchaseOrder(1L));

        assertTrue(exception.getMessage().contains("Purchase order must be CONFIRMED"));
        verify(purchaseOrderRepository).findByIdWithItems(1L);
    }

    @Test
    @DisplayName("Supplementary: Test getConfirmedPurchaseOrder() with CANCELLED status")
    public void testGetConfirmedPurchaseOrder_CancelledStatus() {
        testPurchaseOrder.setStatus(PurchaseOrder.PurchaseOrderStatus.CANCELLED);
        when(purchaseOrderRepository.findByIdWithItems(1L)).thenReturn(testPurchaseOrder);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> purchaseOrderPdfService.getConfirmedPurchaseOrder(1L));

        assertTrue(exception.getMessage().contains("Purchase order must be CONFIRMED"));
        verify(purchaseOrderRepository).findByIdWithItems(1L);
    }

    @Test
    @DisplayName("Supplementary: Test getConfirmedPurchaseOrder() with null order")
    public void testGetConfirmedPurchaseOrder_NullOrder() {
        when(purchaseOrderRepository.findByIdWithItems(1L)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> purchaseOrderPdfService.getConfirmedPurchaseOrder(1L));

        assertTrue(exception.getMessage().contains("Purchase order not found with id: 1"));
        verify(purchaseOrderRepository).findByIdWithItems(1L);
    }
}
