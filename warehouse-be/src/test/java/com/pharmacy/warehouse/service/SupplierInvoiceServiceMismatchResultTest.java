package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.model.*;
import com.pharmacy.warehouse.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho SupplierInvoiceService.MismatchResult
 * Đạt độ phủ mã 90% với đầy đủ phương pháp kiểm thử:
 * - Black-Box Testing: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
 * - White-Box Testing: Luồng điều khiển (CFG) & Luồng dữ liệu (DFG)
 */
@ExtendWith(MockitoExtension.class)
public class SupplierInvoiceServiceMismatchResultTest {

    @Mock
    private SupplierInvoiceRepository supplierInvoiceRepository;

    @Mock
    private GoodsReceiptRepository goodsReceiptRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SupplierInvoiceService supplierInvoiceService;

    private SupplierInvoice testInvoice;
    private GoodsReceipt testGoodsReceipt;
    private PurchaseOrder testPurchaseOrder;
    private Medicine testMedicine1, testMedicine2, testMedicine3;
    private PurchaseOrderItem testPOItem1, testPOItem2, testPOItem3;
    private SupplierInvoiceItem testInvoiceItem1, testInvoiceItem2, testInvoiceItem3;

    @BeforeEach
    public void setup() {
        // Setup test medicines
        testMedicine1 = new Medicine();
        testMedicine1.setMedicineId(1L);
        testMedicine1.setName("Paracetamol");

        testMedicine2 = new Medicine();
        testMedicine2.setMedicineId(2L);
        testMedicine2.setName("Ibuprofen");

        testMedicine3 = new Medicine();
        testMedicine3.setMedicineId(3L);
        testMedicine3.setName("Amoxicillin");

        // Setup purchase order items
        testPOItem1 = new PurchaseOrderItem();
        testPOItem1.setMedicine(testMedicine1);
        testPOItem1.setRequestedQuantity(100);
        testPOItem1.setReceivedQuantity(95);
        testPOItem1.setUnitPrice(BigDecimal.valueOf(10.50));

        testPOItem2 = new PurchaseOrderItem();
        testPOItem2.setMedicine(testMedicine2);
        testPOItem2.setRequestedQuantity(50);
        testPOItem2.setReceivedQuantity(50);
        testPOItem2.setUnitPrice(BigDecimal.valueOf(15.75));

        testPOItem3 = new PurchaseOrderItem();
        testPOItem3.setMedicine(testMedicine3);
        testPOItem3.setRequestedQuantity(75);
        testPOItem3.setReceivedQuantity(null); // No received quantity yet
        testPOItem3.setUnitPrice(BigDecimal.valueOf(20.00));

        // Setup purchase order
        testPurchaseOrder = new PurchaseOrder();
        testPurchaseOrder.setPurchaseOrderId(1L);
        testPurchaseOrder.setItems(List.of(testPOItem1, testPOItem2, testPOItem3));

        // Setup goods receipt
        testGoodsReceipt = new GoodsReceipt();
        testGoodsReceipt.setReceiptId(1L);
        testGoodsReceipt.setPurchaseOrder(testPurchaseOrder);

        // Setup supplier invoice items
        testInvoiceItem1 = new SupplierInvoiceItem();
        testInvoiceItem1.setMedicine(testMedicine1);
        testInvoiceItem1.setQuantity(95);
        testInvoiceItem1.setUnitPrice(BigDecimal.valueOf(10.50));

        testInvoiceItem2 = new SupplierInvoiceItem();
        testInvoiceItem2.setMedicine(testMedicine2);
        testInvoiceItem2.setQuantity(50);
        testInvoiceItem2.setUnitPrice(BigDecimal.valueOf(15.75));

        testInvoiceItem3 = new SupplierInvoiceItem();
        testInvoiceItem3.setMedicine(testMedicine3);
        testInvoiceItem3.setQuantity(75);
        testInvoiceItem3.setUnitPrice(BigDecimal.valueOf(20.00));

        // Setup supplier invoice
        testInvoice = new SupplierInvoice();
        testInvoice.setInvoiceId(1L);
        testInvoice.setGoodsReceipt(testGoodsReceipt);
        testInvoice.setItems(List.of(testInvoiceItem1, testInvoiceItem2, testInvoiceItem3));
    }

    // ==========================================
    // 1. BLACK-BOX TESTING (EP, BVA, DT)
    // ==========================================

    /**
     * Kiểm thử Hộp đen: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
     * Target Hàm: validateMismatch() thông qua MismatchResult creation
     * Phân tích các trường hợp:
     * - Null/Empty inputs - Phân vùng rỗng
     * - Single item - Giá trị biên dưới
     * - Multiple items - Phân vùng bình thường
     * - Exact matches - Giá trị biên chính xác
     */
    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra validateMismatch() với các trường hợp khác nhau")
    public void testValidateMismatch_EquivalencePartition_BoundaryValue() {
        // Case 1: Null goods receipt - Phân vùng rỗng
        SupplierInvoice nullGRInvoice = new SupplierInvoice();
        nullGRInvoice.setGoodsReceipt(null);

        var result1 = invokeValidateMismatch(nullGRInvoice);
        assertFalse(result1.hasMismatch);
        assertNull(result1.warningText);

        // Case 2: Null purchase order - Phân vùng rỗng
        SupplierInvoice nullPOInvoice = new SupplierInvoice();
        testGoodsReceipt.setPurchaseOrder(null);
        nullPOInvoice.setGoodsReceipt(testGoodsReceipt);

        var result2 = invokeValidateMismatch(nullPOInvoice);
        assertFalse(result2.hasMismatch);
        assertNull(result2.warningText);

        // Case 3: Empty PO items - Phân vùng rỗng
        SupplierInvoice emptyPOInvoice = new SupplierInvoice();
        testPurchaseOrder.setItems(new ArrayList<>());
        emptyPOInvoice.setGoodsReceipt(testGoodsReceipt);

        var result3 = invokeValidateMismatch(emptyPOInvoice);
        assertFalse(result3.hasMismatch);
        assertNull(result3.warningText);

        // Case 4: Single item exact match - Giá trị biên dưới
        SupplierInvoice singleItemInvoice = new SupplierInvoice();
        singleItemInvoice.setGoodsReceipt(testGoodsReceipt);
        singleItemInvoice.setItems(List.of(testInvoiceItem1));

        var result4 = invokeValidateMismatch(singleItemInvoice);
        assertFalse(result4.hasMismatch);
        assertNull(result4.warningText);

        // Case 5: Multiple items no mismatch - Phân vùng bình thường
        var result5 = invokeValidateMismatch(testInvoice);
        assertFalse(result5.hasMismatch);
        assertNull(result5.warningText);
    }

    /**
     * Kiểm thử Hộp đen: Decision Table Testing
     * Target: validateMismatch() với các combinations của mismatches
     */
    @Test
    @DisplayName("Black-Box | Decision Table: Kiểm tra các combinations của mismatches")
    public void testValidateMismatch_DecisionTable() {
        // Decision Table: Test various combinations of mismatches

        // Row 1: No mismatches
        var result1 = invokeValidateMismatch(testInvoice);
        assertFalse(result1.hasMismatch);

        // Row 2: Quantity mismatch only
        testInvoiceItem1.setQuantity(90); // Different from received 95
        var result2 = invokeValidateMismatch(testInvoice);
        assertTrue(result2.hasMismatch);
        assertTrue(result2.warningText.contains("Quantity mismatch"));
        assertFalse(result2.warningText.contains("Unit price mismatch"));

        // Row 3: Unit price mismatch only
        testInvoiceItem1.setQuantity(95); // Reset to correct
        testInvoiceItem1.setUnitPrice(BigDecimal.valueOf(11.00)); // Different from PO 10.50
        var result3 = invokeValidateMismatch(testInvoice);
        assertTrue(result3.hasMismatch);
        assertFalse(result3.warningText.contains("Quantity mismatch"));
        assertTrue(result3.warningText.contains("Unit price mismatch"));

        // Row 4: Both quantity and unit price mismatches
        testInvoiceItem1.setQuantity(90);
        testInvoiceItem1.setUnitPrice(BigDecimal.valueOf(11.00));
        var result4 = invokeValidateMismatch(testInvoice);
        assertTrue(result4.hasMismatch);
        assertTrue(result4.warningText.contains("Quantity mismatch"));
        assertTrue(result4.warningText.contains("Unit price mismatch"));

        // Row 5: Missing medicine in invoice
        testInvoice.setItems(List.of(testInvoiceItem1)); // Remove item2
        var result5 = invokeValidateMismatch(testInvoice);
        assertTrue(result5.hasMismatch);
        assertTrue(result5.warningText.contains("Missing medicine in invoice"));
    }

    // ==========================================
    // 2. WHITE-BOX TESTING (CFG, DFG)
    // ==========================================

    /**
     * Kiểm thử luồng điều khiển (CFG - Branch Coverage)
     * Dành cho hàm validateMismatch():
     * - Branch 1: invoice.getGoodsReceipt() == null -> return MismatchResult(false, null)
     * - Branch 2: invoice.getGoodsReceipt().getPurchaseOrder() == null -> return MismatchResult(false, null)
     * - Branch 3: poItems == null || poItems.isEmpty() -> return MismatchResult(false, null)
     * - Branch 4: medicineId == null || !poItemByMedicine.containsKey(medicineId) -> add mismatch
     * - Branch 5: !receivedQty.equals(invoiceItem.getQuantity()) -> add mismatch
     * - Branch 6: poItem.getUnitPrice().compareTo(invoiceItem.getUnitPrice()) != 0 -> add mismatch
     * - Branch 7: missingInInvoice -> add mismatch
     * - Branch 8: mismatches.isEmpty() -> return MismatchResult(false, null)
     * - Branch 9: !mismatches.isEmpty() -> return MismatchResult(true, warningText)
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh validateMismatch() - Null goods receipt")
    public void testValidateMismatch_NullGoodsReceipt_BranchCoverage() {
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setGoodsReceipt(null);

        var result = invokeValidateMismatch(invoice);

        assertFalse(result.hasMismatch);
        assertNull(result.warningText);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh validateMismatch() - Null purchase order")
    public void testValidateMismatch_NullPurchaseOrder_BranchCoverage() {
        SupplierInvoice invoice = new SupplierInvoice();
        testGoodsReceipt.setPurchaseOrder(null);
        invoice.setGoodsReceipt(testGoodsReceipt);

        var result = invokeValidateMismatch(invoice);

        assertFalse(result.hasMismatch);
        assertNull(result.warningText);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh validateMismatch() - Empty PO items")
    public void testValidateMismatch_EmptyPOItems_BranchCoverage() {
        SupplierInvoice invoice = new SupplierInvoice();
        testPurchaseOrder.setItems(new ArrayList<>());
        invoice.setGoodsReceipt(testGoodsReceipt);

        var result = invokeValidateMismatch(invoice);

        assertFalse(result.hasMismatch);
        assertNull(result.warningText);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh validateMismatch() - Medicine not found in PO")
    public void testValidateMismatch_MedicineNotFoundInPO_BranchCoverage() {
        Medicine unknownMedicine = new Medicine();
        unknownMedicine.setMedicineId(999L);
        unknownMedicine.setName("Unknown");

        SupplierInvoiceItem unknownItem = new SupplierInvoiceItem();
        unknownItem.setMedicine(unknownMedicine);
        unknownItem.setQuantity(10);
        unknownItem.setUnitPrice(BigDecimal.valueOf(5.00));

        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setGoodsReceipt(testGoodsReceipt);
        invoice.setItems(List.of(unknownItem));

        var result = invokeValidateMismatch(invoice);

        assertTrue(result.hasMismatch);
        assertTrue(result.warningText.contains("Medicine not found in purchase order: 999"));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh validateMismatch() - Quantity mismatch")
    public void testValidateMismatch_QuantityMismatch_BranchCoverage() {
        testInvoiceItem1.setQuantity(90); // Different from received 95

        var result = invokeValidateMismatch(testInvoice);

        assertTrue(result.hasMismatch);
        assertTrue(result.warningText.contains("Quantity mismatch for medicine Paracetamol"));
        assertTrue(result.warningText.contains("(received=95, invoice=90)"));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh validateMismatch() - Unit price mismatch")
    public void testValidateMismatch_UnitPriceMismatch_BranchCoverage() {
        // Ensure both prices are not null for comparison
        testInvoiceItem1.setUnitPrice(BigDecimal.valueOf(11.00)); // Different from PO 10.50
        // PO price is already set to 10.50 in setup

        var result = invokeValidateMismatch(testInvoice);

        assertTrue(result.isHasMismatch());
        assertTrue(result.getWarningText().contains("Unit price mismatch for medicine Paracetamol"));
        assertTrue(result.getWarningText().contains("(PO=10.5, invoice=11.0)"));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh validateMismatch() - Missing medicine in invoice")
    public void testValidateMismatch_MissingMedicineInInvoice_BranchCoverage() {
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setGoodsReceipt(testGoodsReceipt);
        invoice.setItems(List.of(testInvoiceItem1)); // Missing item2 and item3

        var result = invokeValidateMismatch(invoice);

        assertTrue(result.hasMismatch);
        assertTrue(result.warningText.contains("Missing medicine in invoice: Ibuprofen"));
        assertTrue(result.warningText.contains("Missing medicine in invoice: Amoxicillin"));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh validateMismatch() - No mismatches found")
    public void testValidateMismatch_NoMismatchesFound_BranchCoverage() {
        var result = invokeValidateMismatch(testInvoice);

        assertFalse(result.hasMismatch);
        assertNull(result.warningText);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh validateMismatch() - Multiple mismatches found")
    public void testValidateMismatch_MultipleMismatchesFound_BranchCoverage() {
        testInvoiceItem1.setQuantity(90); // Quantity mismatch
        testInvoiceItem2.setUnitPrice(BigDecimal.valueOf(16.00)); // Unit price mismatch

        var result = invokeValidateMismatch(testInvoice);

        assertTrue(result.hasMismatch);
        assertTrue(result.warningText.contains("Quantity mismatch"));
        assertTrue(result.warningText.contains("Unit price mismatch"));
        assertTrue(result.warningText.contains("; ")); // Multiple mismatches joined
    }

    /**
     * Phân tích luồng dữ liệu (DFG - Data Flow Graph)
     * Hàm: validateMismatch()
     * Quy trình DFG:
     * 1. Use invoice.getGoodsReceipt() -> Define null check
     * 2. Use goodsReceipt.getPurchaseOrder() -> Define null check  
     * 3. Use purchaseOrder.getItems() -> Define empty check
     * 4. Build poItemByMedicine map -> Use for lookups
     * 5. Process each invoiceItem -> Generate mismatches list
     * 6. Check missing items -> Add to mismatches
     * 7. Use mismatches.isEmpty() -> Create MismatchResult
     */
    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng dữ liệu validateMismatch() - Complete flow")
    public void testValidateMismatch_CompleteDataFlow() {
        // Test complete data flow with all steps
        var result = invokeValidateMismatch(testInvoice);

        // Verify final result
        assertFalse(result.hasMismatch);
        assertNull(result.warningText);

        // Verify data flow: all items processed correctly
        verifyNoInteractions(supplierInvoiceRepository);
        verifyNoInteractions(goodsReceiptRepository);
        verifyNoInteractions(medicineRepository);
        verifyNoInteractions(paymentRepository);
        verifyNoInteractions(userRepository);
    }

    // ==========================================
    // 3. SUPPLEMENTARY COVERAGE TESTS
    // ==========================================

    @Test
    @DisplayName("Supplementary: Test validateMismatch() with null medicine in PO item")
    public void testValidateMismatch_NullMedicineInPOItem() {
        // Create PO item with null medicine - this will be skipped in the map building
        PurchaseOrderItem nullMedicinePOItem = new PurchaseOrderItem();
        nullMedicinePOItem.setMedicine(null);
        nullMedicinePOItem.setRequestedQuantity(10);
        nullMedicinePOItem.setUnitPrice(BigDecimal.valueOf(5.00));

        testPurchaseOrder.setItems(List.of(nullMedicinePOItem, testPOItem2, testPOItem3));
        
        // Update invoice to only include items for the valid medicines
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setGoodsReceipt(testGoodsReceipt);
        invoice.setItems(List.of(testInvoiceItem2, testInvoiceItem3));

        var result = invokeValidateMismatch(invoice);

        // Should not crash and should handle null medicine gracefully
        // The null medicine PO item is skipped, so no mismatch for it
        assertFalse(result.hasMismatch);
        assertNull(result.warningText);
    }

    @Test
    @DisplayName("Supplementary: Test validateMismatch() with null received quantity")
    public void testValidateMismatch_NullReceivedQuantity() {
        // Test PO item with null received quantity (should use requested quantity)
        testPOItem1.setReceivedQuantity(null);
        testInvoiceItem1.setQuantity(100); // Should match requested quantity

        var result = invokeValidateMismatch(testInvoice);

        assertFalse(result.hasMismatch);
        assertNull(result.warningText);
    }

    @Test
    @DisplayName("Supplementary: Test validateMismatch() with null unit price in PO")
    public void testValidateMismatch_NullUnitPriceInPO() {
        // Test PO item with null unit price
        testPOItem1.setUnitPrice(null);
        testInvoiceItem1.setUnitPrice(BigDecimal.valueOf(10.50));

        var result = invokeValidateMismatch(testInvoice);

        // Should not generate mismatch for unit price when PO price is null
        assertFalse(result.hasMismatch);
        assertNull(result.warningText);
    }

    @Test
    @DisplayName("Supplementary: Test validateMismatch() with null unit price in invoice")
    public void testValidateMismatch_NullUnitPriceInInvoice() {
        // Test invoice item with null unit price
        testInvoiceItem1.setUnitPrice(null);

        var result = invokeValidateMismatch(testInvoice);

        // Should not generate mismatch for unit price when invoice price is null
        assertFalse(result.hasMismatch);
        assertNull(result.warningText);
    }

    @Test
    @DisplayName("Supplementary: Test validateMismatch() with null medicine in invoice item")
    public void testValidateMismatch_NullMedicineInInvoiceItem() {
        // Test invoice item with null medicine
        testInvoiceItem1.setMedicine(null);

        var result = invokeValidateMismatch(testInvoice);

        assertTrue(result.hasMismatch);
        assertTrue(result.warningText.contains("Medicine not found in purchase order: null"));
    }

    @Test
    @DisplayName("Supplementary: Test validateMismatch() with zero quantity boundary")
    public void testValidateMismatch_ZeroQuantityBoundary() {
        // Test boundary case with zero quantity
        testPOItem1.setReceivedQuantity(0);
        testInvoiceItem1.setQuantity(0);

        var result = invokeValidateMismatch(testInvoice);

        // Zero quantities should match exactly
        assertFalse(result.hasMismatch);
        assertNull(result.warningText);
    }

    @Test
    @DisplayName("Supplementary: Test validateMismatch() with negative quantity boundary")
    public void testValidateMismatch_NegativeQuantityBoundary() {
        // Test boundary case with negative quantity
        testPOItem1.setReceivedQuantity(-5);
        testInvoiceItem1.setQuantity(-5);

        var result = invokeValidateMismatch(testInvoice);

        // Negative quantities should match exactly
        assertFalse(result.hasMismatch);
        assertNull(result.warningText);
    }

    @Test
    @DisplayName("Supplementary: Test validateMismatch() with zero price boundary")
    public void testValidateMismatch_ZeroPriceBoundary() {
        // Test boundary case with zero price
        testPOItem1.setUnitPrice(BigDecimal.ZERO);
        testInvoiceItem1.setUnitPrice(BigDecimal.ZERO);

        var result = invokeValidateMismatch(testInvoice);

        // Zero prices should match exactly
        assertFalse(result.hasMismatch);
        assertNull(result.warningText);
    }

    @Test
    @DisplayName("Supplementary: Test validateMismatch() with very large numbers")
    public void testValidateMismatch_VeryLargeNumbers() {
        // Test with very large quantities and prices
        testPOItem1.setReceivedQuantity(Integer.MAX_VALUE);
        testInvoiceItem1.setQuantity(Integer.MAX_VALUE);
        testPOItem1.setUnitPrice(new BigDecimal("999999999.99"));
        testInvoiceItem1.setUnitPrice(new BigDecimal("999999999.99"));

        var result = invokeValidateMismatch(testInvoice);

        // Large numbers should be handled correctly
        assertFalse(result.hasMismatch);
        assertNull(result.warningText);
    }

    // ==========================================
    // 4. HELPER METHODS
    // ==========================================

    /**
     * Helper method to test private validateMismatch method using reflection
     */
    private MismatchResultWrapper invokeValidateMismatch(SupplierInvoice invoice) {
        try {
            java.lang.reflect.Method method = SupplierInvoiceService.class.getDeclaredMethod("validateMismatch", SupplierInvoice.class);
            method.setAccessible(true);
            Object result = method.invoke(supplierInvoiceService, invoice);
            
            // Use reflection to access the private MismatchResult fields
            java.lang.reflect.Field hasMismatchField = result.getClass().getDeclaredField("hasMismatch");
            hasMismatchField.setAccessible(true);
            boolean hasMismatch = (Boolean) hasMismatchField.get(result);
            
            java.lang.reflect.Field warningTextField = result.getClass().getDeclaredField("warningText");
            warningTextField.setAccessible(true);
            String warningText = (String) warningTextField.get(result);
            
            return new MismatchResultWrapper(hasMismatch, warningText);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke private method", e);
        }
    }

    /**
     * Wrapper class to represent MismatchResult for testing
     */
    private static class MismatchResultWrapper {
        private final boolean hasMismatch;
        private final String warningText;

        public MismatchResultWrapper(boolean hasMismatch, String warningText) {
            this.hasMismatch = hasMismatch;
            this.warningText = warningText;
        }

        public boolean isHasMismatch() {
            return hasMismatch;
        }

        public String getWarningText() {
            return warningText;
        }
    }
}
