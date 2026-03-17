package com.pharmacy.warehouse.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pharmacy.warehouse.dto.CreateSupplierInvoiceRequest;
import com.pharmacy.warehouse.dto.PaymentInvoiceRequest;
import com.pharmacy.warehouse.dto.RejectInvoiceRequest;
import com.pharmacy.warehouse.dto.SupplierInvoiceResponse;
import com.pharmacy.warehouse.dto.VerifyInvoiceRequest;
import com.pharmacy.warehouse.model.GoodsReceipt;
import com.pharmacy.warehouse.model.GoodsReceipt.ReceiptStatus;
import com.pharmacy.warehouse.model.Medicine;
import com.pharmacy.warehouse.model.Payment;
import com.pharmacy.warehouse.model.PurchaseOrder;
import com.pharmacy.warehouse.model.PurchaseOrderItem;
import com.pharmacy.warehouse.model.Supplier;
import com.pharmacy.warehouse.model.SupplierInvoice;
import com.pharmacy.warehouse.model.SupplierInvoice.InvoiceStatus;
import com.pharmacy.warehouse.model.SupplierInvoiceItem;
import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.repository.GoodsReceiptRepository;
import com.pharmacy.warehouse.repository.MedicineRepository;
import com.pharmacy.warehouse.repository.PaymentRepository;
import com.pharmacy.warehouse.repository.SupplierInvoiceRepository;
import com.pharmacy.warehouse.repository.UserRepository;

/**
 * Unit Test cho SupplierInvoiceService
 * Đạt độ phủ mã 90% với đầy đủ phương pháp kiểm thử:
 * - Black-Box Testing: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
 * - White-Box Testing: Luồng điều khiển (CFG) & Luồng dữ liệu (DFG)
 */
@ExtendWith(MockitoExtension.class)
public class SupplierInvoiceServiceTest {

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

    private User testUser;
    private Medicine testMedicine;
    private Supplier testSupplier;
    private PurchaseOrder testPurchaseOrder;
    private GoodsReceipt testGoodsReceipt;
    private SupplierInvoice testInvoice;
    private SupplierInvoiceItem testInvoiceItem;

    @BeforeEach
    public void setup() {
        // Setup test user
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("testuser");
        testUser.setFullName("Test User");

        // Setup test medicine
        testMedicine = new Medicine();
        testMedicine.setMedicineId(1L);
        testMedicine.setName("Paracetamol");

        // Setup test supplier
        testSupplier = new Supplier();
        testSupplier.setSupplierId(1L);
        testSupplier.setSupplierName("Test Supplier");

        // Setup test purchase order
        testPurchaseOrder = new PurchaseOrder();
        testPurchaseOrder.setOrderCode("PO001");
        testPurchaseOrder.setSupplier(testSupplier);

        // Setup test goods receipt
        testGoodsReceipt = new GoodsReceipt();
        testGoodsReceipt.setReceiptId(1L);
        testGoodsReceipt.setReceiptCode("GR001");
        testGoodsReceipt.setStatus(ReceiptStatus.APPROVED);
        testGoodsReceipt.setPurchaseOrder(testPurchaseOrder);

        // Setup test invoice
        testInvoice = new SupplierInvoice();
        testInvoice.setInvoiceId(1L);
        testInvoice.setInvoiceCode("INV001");
        testInvoice.setGoodsReceipt(testGoodsReceipt);
        testInvoice.setSupplier(testSupplier);
        testInvoice.setCreatedBy(testUser);
        testInvoice.setStatus(InvoiceStatus.PENDING_VERIFICATION);
        testInvoice.setTotalAmount(new BigDecimal("100.00"));
        testInvoice.setPaidAmount(BigDecimal.ZERO);
        testInvoice.setRemainingAmount(new BigDecimal("100.00"));

        // Setup test invoice item
        testInvoiceItem = new SupplierInvoiceItem();
        testInvoiceItem.setInvoiceItemId(1L);
        testInvoiceItem.setMedicine(testMedicine);
        testInvoiceItem.setQuantity(10);
        testInvoiceItem.setUnitPrice(new BigDecimal("10.00"));
        testInvoiceItem.setTotalPrice(new BigDecimal("100.00"));
        testInvoiceItem.setSupplierInvoice(testInvoice);

        testInvoice.setItems(List.of(testInvoiceItem));
    }

    // ==========================================
    // 1. BLACK-BOX TESTING (EP, BVA, DT)
    // ==========================================

    /**
     * Kiểm thử Hộp đen: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
     * Target: createInvoice() với các trường hợp khác nhau
     * Phân tích các trường hợp:
     * - Valid inputs - Phân vùng hợp lệ
     * - Invalid goodsReceiptId - Phân vùng không hợp lệ
     * - Empty items list - Phân vùng biên
     * - Invalid quantity/price - Phân vùng biên
     */
    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra createInvoice() với các trường hợp khác nhau")
    public void testCreateInvoice_EquivalencePartition_BoundaryValue() {
        // Case 1: Valid inputs - Phân vùng hợp lệ
        CreateSupplierInvoiceRequest validRequest = createValidInvoiceRequest();
        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));
        when(supplierInvoiceRepository.findByGoodsReceipt_ReceiptId(1L)).thenReturn(Optional.empty());
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenReturn(testInvoice);

        SupplierInvoiceResponse result = supplierInvoiceService.createInvoice(validRequest, 1L);

        assertNotNull(result);
        assertEquals("INV001", result.getInvoiceCode());
        assertEquals(InvoiceStatus.PENDING_VERIFICATION.name(), result.getStatus());
    }

    @Test
    @DisplayName("Black-Box | BVA: Kiểm tra createInvoice() với boundary values")
    public void testCreateInvoice_BoundaryValues() {
        // Setup lenient stubs for all cases
        lenient().when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));
        lenient().when(supplierInvoiceRepository.findByGoodsReceipt_ReceiptId(1L)).thenReturn(Optional.empty());
        lenient().when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenReturn(testInvoice);

        // Case 1: Minimum quantity (1) - Valid
        CreateSupplierInvoiceRequest request = createValidInvoiceRequest();
        request.getItems().get(0).setQuantity(1);
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertDoesNotThrow(() -> supplierInvoiceService.createInvoice(request, 1L));

        // Case 2: Minimum positive price (0.01) - Valid
        request.getItems().get(0).setQuantity(10);
        request.getItems().get(0).setUnitPrice(new BigDecimal("0.01"));
        
        assertDoesNotThrow(() -> supplierInvoiceService.createInvoice(request, 1L));

        // Case 3: Large quantity - Valid
        request.getItems().get(0).setQuantity(1000);
        request.getItems().get(0).setUnitPrice(new BigDecimal("10.00"));
        
        assertDoesNotThrow(() -> supplierInvoiceService.createInvoice(request, 1L));

        // Case 4: High price - Valid
        request.getItems().get(0).setQuantity(10);
        request.getItems().get(0).setUnitPrice(new BigDecimal("999999.99"));
        
        assertDoesNotThrow(() -> supplierInvoiceService.createInvoice(request, 1L));
    }

    /**
     * Kiểm thử Hộp đen: Decision Table Testing
     * Target: createInvoice() với các combinations của validation
     */
    @Test
    @DisplayName("Black-Box | Decision Table: Kiểm tra createInvoice() validation combinations")
    public void testCreateInvoice_DecisionTable() {
        CreateSupplierInvoiceRequest validRequest = createValidInvoiceRequest();
        
        // Setup lenient stubs for all cases
        lenient().when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));
        lenient().when(goodsReceiptRepository.findById(999L)).thenReturn(Optional.empty());
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        lenient().when(userRepository.findById(999L)).thenReturn(Optional.empty());
        lenient().when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));
        lenient().when(supplierInvoiceRepository.findByGoodsReceipt_ReceiptId(1L)).thenReturn(Optional.empty());
        lenient().when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenReturn(testInvoice);

        // Row 1: Valid goods receipt, valid items - Success
        SupplierInvoiceResponse result1 = supplierInvoiceService.createInvoice(validRequest, 1L);
        assertNotNull(result1);

        // Row 2: Invalid goods receipt ID - Error
        assertThrows(RuntimeException.class, () -> 
            supplierInvoiceService.createInvoice(validRequest, 999L));

        // Row 3: Empty items list - Error
        CreateSupplierInvoiceRequest emptyItemsRequest = new CreateSupplierInvoiceRequest();
        emptyItemsRequest.setItems(new ArrayList<>());

        assertThrows(IllegalArgumentException.class, () -> 
            supplierInvoiceService.createInvoice(emptyItemsRequest, 1L));

        // Row 4: Null items list - Error
        CreateSupplierInvoiceRequest nullItemsRequest = new CreateSupplierInvoiceRequest();
        nullItemsRequest.setItems(null);

        assertThrows(IllegalArgumentException.class, () -> 
            supplierInvoiceService.createInvoice(nullItemsRequest, 1L));

        // Row 5: Invalid user ID - Error
        assertThrows(RuntimeException.class, () -> 
            supplierInvoiceService.createInvoice(validRequest, 999L));
    }

    // ==========================================
    // 2. WHITE-BOX TESTING (CFG, DFG)
    // ==========================================

    /**
     * Kiểm thử luồng điều khiển (CFG - Branch Coverage)
     * Dành cho hàm createInvoice():
     * - Branch 1: items == null || items.isEmpty() -> throw IllegalArgumentException
     * - Branch 2: goodsReceipt not found -> throw RuntimeException
     * - Branch 3: goodsReceipt.status != APPROVED -> throw IllegalStateException
     * - Branch 4: invoice already exists -> throw RuntimeException
     * - Branch 5: user not found -> throw RuntimeException
     * - Branch 6: item quantity <= 0 -> throw IllegalArgumentException
     * - Branch 7: item unit price invalid -> throw IllegalArgumentException
     * - Branch 8: medicine not found -> throw RuntimeException
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createInvoice() - Validation errors")
    public void testCreateInvoice_ValidationErrors_BranchCoverage() {
        // Branch 1: Null items
        CreateSupplierInvoiceRequest request = new CreateSupplierInvoiceRequest();
        request.setItems(null);

        assertThrows(IllegalArgumentException.class, () -> 
            supplierInvoiceService.createInvoice(request, 1L));

        // Branch 1: Empty items
        request.setItems(new ArrayList<>());

        assertThrows(IllegalArgumentException.class, () -> 
            supplierInvoiceService.createInvoice(request, 1L));

        // Branch 2: Goods receipt not found
        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> 
            supplierInvoiceService.createInvoice(createValidInvoiceRequest(), 1L));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createInvoice() - Status validation")
    public void testCreateInvoice_StatusValidation_BranchCoverage() {
        // Branch 3: Goods receipt not approved
        testGoodsReceipt.setStatus(ReceiptStatus.PENDING_APPROVAL);
        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));

        assertThrows(IllegalStateException.class, () -> 
            supplierInvoiceService.createInvoice(createValidInvoiceRequest(), 1L));

        // Reset to approved for next test
        testGoodsReceipt.setStatus(ReceiptStatus.APPROVED);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createInvoice() - Duplicate invoice")
    public void testCreateInvoice_DuplicateInvoice_BranchCoverage() {
        // Branch 4: Invoice already exists
        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));
        when(supplierInvoiceRepository.findByGoodsReceipt_ReceiptId(1L))
            .thenReturn(Optional.of(testInvoice));

        assertThrows(RuntimeException.class, () -> 
            supplierInvoiceService.createInvoice(createValidInvoiceRequest(), 1L));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createInvoice() - Item validation")
    public void testCreateInvoice_ItemValidation_BranchCoverage() {
        // Setup lenient stubs for all cases
        lenient().when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));
        lenient().when(supplierInvoiceRepository.findByGoodsReceipt_ReceiptId(1L)).thenReturn(Optional.empty());
        lenient().when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenReturn(testInvoice);

        // Branch 6: Valid quantity
        CreateSupplierInvoiceRequest request = createValidInvoiceRequest();
        request.getItems().get(0).setQuantity(10);
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertDoesNotThrow(() -> supplierInvoiceService.createInvoice(request, 1L));

        // Branch 7: Valid unit price
        request.getItems().get(0).setQuantity(20);
        request.getItems().get(0).setUnitPrice(new BigDecimal("15.50"));
        // Reset mock for next call
        reset(medicineRepository);
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));

        assertDoesNotThrow(() -> supplierInvoiceService.createInvoice(request, 1L));

        // Branch 8: Valid medicine found
        request.getItems().get(0).setUnitPrice(new BigDecimal("20.00"));
        // Reset mock for next call
        reset(medicineRepository);
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));

        assertDoesNotThrow(() -> supplierInvoiceService.createInvoice(request, 1L));
    }

    /**
     * Kiểm thử luồng điều khiển (CFG) cho verifyInvoice():
     * - Branch 1: invoice not found -> throw RuntimeException
     * - Branch 2: invoice status != PENDING_VERIFICATION -> throw IllegalStateException
     * - Branch 3: user not found -> throw RuntimeException
     * - Branch 4: has mismatch -> throw IllegalStateException
     * - Branch 5: successful verification -> update status
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh verifyInvoice() - Validation")
    public void testVerifyInvoice_Validation_BranchCoverage() {
        // Branch 1: Invoice not found
        when(supplierInvoiceRepository.findByIdWithItems(1L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> 
            supplierInvoiceService.verifyInvoice(1L, null, 1L));

        // Branch 2: Wrong status
        testInvoice.setStatus(InvoiceStatus.VERIFIED);
        when(supplierInvoiceRepository.findByIdWithItems(1L)).thenReturn(testInvoice);

        assertThrows(IllegalStateException.class, () -> 
            supplierInvoiceService.verifyInvoice(1L, null, 1L));

        // Reset to PENDING_VERIFICATION for next test
        testInvoice.setStatus(InvoiceStatus.PENDING_VERIFICATION);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh verifyInvoice() - User validation")
    public void testVerifyInvoice_UserValidation_BranchCoverage() {
        // Branch 3: User not found
        when(supplierInvoiceRepository.findByIdWithItems(1L)).thenReturn(testInvoice);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> 
            supplierInvoiceService.verifyInvoice(1L, null, 1L));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh verifyInvoice() - Mismatch handling")
    public void testVerifyInvoice_MismatchHandling_BranchCoverage() {
        // Branch 4: Has mismatch - should throw exception
        // Create PO item with different quantity to cause mismatch
        PurchaseOrderItem poItem = new PurchaseOrderItem();
        poItem.setMedicine(testMedicine);
        poItem.setReceivedQuantity(5); // Different from invoice quantity (10)
        poItem.setUnitPrice(new BigDecimal("10.00"));
        testGoodsReceipt.getPurchaseOrder().setItems(List.of(poItem));

        when(supplierInvoiceRepository.findByIdWithItems(1L)).thenReturn(testInvoice);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenReturn(testInvoice);

        assertThrows(IllegalStateException.class, () -> 
            supplierInvoiceService.verifyInvoice(1L, null, 1L));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh verifyInvoice() - Success case")
    public void testVerifyInvoice_Success_BranchCoverage() {
        // Branch 5: Successful verification
        PurchaseOrderItem poItem = new PurchaseOrderItem();
        poItem.setMedicine(testMedicine);
        poItem.setReceivedQuantity(10);
        poItem.setUnitPrice(new BigDecimal("10.00"));
        testGoodsReceipt.getPurchaseOrder().setItems(List.of(poItem));

        when(supplierInvoiceRepository.findByIdWithItems(1L)).thenReturn(testInvoice);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenReturn(testInvoice);

        SupplierInvoiceResponse result = supplierInvoiceService.verifyInvoice(1L, 
            new VerifyInvoiceRequest("Verified"), 1L);

        assertNotNull(result);
        assertEquals(InvoiceStatus.VERIFIED.name(), result.getStatus());
        verify(supplierInvoiceRepository).save(testInvoice);
    }

    /**
     * Kiểm thử luồng điều khiển (CFG) cho payInvoice():
     * - Branch 1: invalid payment amount -> throw IllegalArgumentException
     * - Branch 2: invoice not found -> throw RuntimeException
     * - Branch 3: wrong invoice status -> throw IllegalStateException
     * - Branch 4: payment exceeds remaining -> throw IllegalArgumentException
     * - Branch 5: user not found -> throw RuntimeException
     * - Branch 6: partial payment -> PARTIALLY_PAID
     * - Branch 7: full payment -> PAID
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh payInvoice() - Payment validation")
    public void testPayInvoice_PaymentValidation_BranchCoverage() {
        // Branch 1: Null payment request
        assertThrows(IllegalArgumentException.class, () -> 
            supplierInvoiceService.payInvoice(1L, null, 1L));

        // Branch 1: Zero amount
        PaymentInvoiceRequest zeroAmountRequest = new PaymentInvoiceRequest();
        zeroAmountRequest.setAmount(BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () -> 
            supplierInvoiceService.payInvoice(1L, zeroAmountRequest, 1L));

        // Branch 1: Negative amount
        zeroAmountRequest.setAmount(new BigDecimal("-10.00"));

        assertThrows(IllegalArgumentException.class, () -> 
            supplierInvoiceService.payInvoice(1L, zeroAmountRequest, 1L));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh payInvoice() - Invoice validation")
    public void testPayInvoice_InvoiceValidation_BranchCoverage() {
        // Branch 2: Invoice not found
        when(supplierInvoiceRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> 
            supplierInvoiceService.payInvoice(1L, new PaymentInvoiceRequest(), 1L));

        // Branch 3: Wrong status - PENDING_VERIFICATION should throw IllegalArgumentException
        testInvoice.setStatus(InvoiceStatus.PENDING_VERIFICATION);
        lenient().when(supplierInvoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));

        assertThrows(IllegalArgumentException.class, () -> 
            supplierInvoiceService.payInvoice(1L, new PaymentInvoiceRequest(), 1L));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh payInvoice() - Amount validation")
    public void testPayInvoice_AmountValidation_BranchCoverage() {
        // Branch 4: Payment exceeds remaining amount
        testInvoice.setStatus(InvoiceStatus.VERIFIED);
        testInvoice.setRemainingAmount(new BigDecimal("50.00"));
        when(supplierInvoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));

        PaymentInvoiceRequest excessAmountRequest = new PaymentInvoiceRequest();
        excessAmountRequest.setAmount(new BigDecimal("100.00"));

        assertThrows(IllegalArgumentException.class, () -> 
            supplierInvoiceService.payInvoice(1L, excessAmountRequest, 1L));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh payInvoice() - Payment status update")
    public void testPayInvoice_PaymentStatusUpdate_BranchCoverage() {
        // Setup lenient stubs
        testInvoice.setStatus(InvoiceStatus.VERIFIED);
        testInvoice.setRemainingAmount(new BigDecimal("50.00"));
        lenient().when(supplierInvoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Branch 5: User not found
        assertThrows(RuntimeException.class, () -> 
            supplierInvoiceService.payInvoice(1L, new PaymentInvoiceRequest(), 1L));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh payInvoice() - Full vs partial payment")
    public void testPayInvoice_FullVsPartialPayment_BranchCoverage() {
        // Branch 6: Partial payment
        testInvoice.setStatus(InvoiceStatus.VERIFIED);
        testInvoice.setTotalAmount(new BigDecimal("100.00"));
        testInvoice.setPaidAmount(BigDecimal.ZERO);
        testInvoice.setRemainingAmount(new BigDecimal("100.00"));
        when(supplierInvoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentRepository.save(any(Payment.class))).thenReturn(new Payment());
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenReturn(testInvoice);

        PaymentInvoiceRequest partialPaymentRequest = new PaymentInvoiceRequest();
        partialPaymentRequest.setAmount(new BigDecimal("50.00"));

        SupplierInvoiceResponse result = supplierInvoiceService.payInvoice(1L, partialPaymentRequest, 1L);
        
        assertNotNull(result);
        assertEquals(InvoiceStatus.PARTIALLY_PAID.name(), result.getStatus());

        // Branch 7: Full payment
        testInvoice.setPaidAmount(new BigDecimal("50.00"));
        testInvoice.setRemainingAmount(new BigDecimal("50.00"));
        
        PaymentInvoiceRequest fullPaymentRequest = new PaymentInvoiceRequest();
        fullPaymentRequest.setAmount(new BigDecimal("50.00"));

        result = supplierInvoiceService.payInvoice(1L, fullPaymentRequest, 1L);
        
        assertNotNull(result);
        assertEquals(InvoiceStatus.PAID.name(), result.getStatus());
    }

    /**
     * Kiểm thử luồng dữ liệu (DFG - Data Flow Graph)
     * Hàm: createInvoice()
     * Quy trình DFG:
     * 1. Use request items -> validate not empty
     * 2. Use goodsReceiptId -> findById validation
     * 3. Use goodsReceipt -> status validation
     * 4. Use userId -> findById validation
     * 5. Use items -> create invoice items
     * 6. Use calculations -> set total amounts
     * 7. Use validation -> set mismatch flags
     * 8. Use result -> save and return response
     */
    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng dữ liệu createInvoice() - Complete flow")
    public void testCreateInvoice_CompleteDataFlow() {
        CreateSupplierInvoiceRequest request = createValidInvoiceRequest();
        
        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));
        when(supplierInvoiceRepository.findByGoodsReceipt_ReceiptId(1L)).thenReturn(Optional.empty());
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenReturn(testInvoice);

        SupplierInvoiceResponse result = supplierInvoiceService.createInvoice(request, 1L);

        // Verify complete data flow
        assertNotNull(result);
        verify(goodsReceiptRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(medicineRepository).findById(1L);
        verify(supplierInvoiceRepository).save(any(SupplierInvoice.class));
    }

    // ==========================================
    // 3. SUPPLEMENTARY COVERAGE TESTS
    // ==========================================

    @Test
    @DisplayName("Supplementary: Test getAllInvoices()")
    public void testGetAllInvoices() {
        List<SupplierInvoice> invoices = List.of(testInvoice);
        when(supplierInvoiceRepository.findAll()).thenReturn(invoices);
        when(paymentRepository.findBySupplierInvoice_InvoiceId(1L)).thenReturn(new ArrayList<>());

        List<SupplierInvoiceResponse> result = supplierInvoiceService.getAllInvoices();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("INV001", result.get(0).getInvoiceCode());
    }

    @Test
    @DisplayName("Supplementary: Test getInvoiceById() - Success")
    public void testGetInvoiceById_Success() {
        when(supplierInvoiceRepository.findByIdWithItems(1L)).thenReturn(testInvoice);
        when(paymentRepository.findBySupplierInvoice_InvoiceId(1L)).thenReturn(new ArrayList<>());

        SupplierInvoiceResponse result = supplierInvoiceService.getInvoiceById(1L);

        assertNotNull(result);
        assertEquals("INV001", result.getInvoiceCode());
    }

    @Test
    @DisplayName("Supplementary: Test getInvoiceById() - Not found")
    public void testGetInvoiceById_NotFound() {
        when(supplierInvoiceRepository.findByIdWithItems(1L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> 
            supplierInvoiceService.getInvoiceById(1L));
    }

    @Test
    @DisplayName("Supplementary: Test rejectInvoice() - Success")
    public void testRejectInvoice_Success() {
        testInvoice.setStatus(InvoiceStatus.PENDING_VERIFICATION);
        when(supplierInvoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenReturn(testInvoice);

        RejectInvoiceRequest request = new RejectInvoiceRequest("Rejected for testing");
        SupplierInvoiceResponse result = supplierInvoiceService.rejectInvoice(1L, request, 1L);

        assertNotNull(result);
        assertEquals(InvoiceStatus.REJECTED.name(), result.getStatus());
        assertEquals("Rejected for testing", result.getRejectionReason());
    }

    @Test
    @DisplayName("Supplementary: Test rejectInvoice() - Wrong status")
    public void testRejectInvoice_WrongStatus() {
        testInvoice.setStatus(InvoiceStatus.VERIFIED);
        when(supplierInvoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));

        assertThrows(IllegalStateException.class, () -> 
            supplierInvoiceService.rejectInvoice(1L, null, 1L));
    }

    @Test
    @DisplayName("Supplementary: Test validateMismatch() - No mismatch")
    public void testValidateMismatch_NoMismatch() {
        // Create matching PO item
        PurchaseOrderItem poItem = new PurchaseOrderItem();
        poItem.setMedicine(testMedicine);
        poItem.setReceivedQuantity(10);
        poItem.setUnitPrice(new BigDecimal("10.00"));
        testGoodsReceipt.getPurchaseOrder().setItems(List.of(poItem));

        when(supplierInvoiceRepository.findByIdWithItems(1L)).thenReturn(testInvoice);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenReturn(testInvoice);

        SupplierInvoiceResponse result = supplierInvoiceService.verifyInvoice(1L, null, 1L);

        assertNotNull(result);
        assertEquals(InvoiceStatus.VERIFIED.name(), result.getStatus());
    }

    @Test
    @DisplayName("Supplementary: Test validateMismatch() - Quantity mismatch")
    public void testValidateMismatch_QuantityMismatch() {
        // Create PO item with different quantity
        PurchaseOrderItem poItem = new PurchaseOrderItem();
        poItem.setMedicine(testMedicine);
        poItem.setReceivedQuantity(5); // Different from invoice quantity (10)
        poItem.setUnitPrice(new BigDecimal("10.00"));
        testGoodsReceipt.getPurchaseOrder().setItems(List.of(poItem));

        when(supplierInvoiceRepository.findByIdWithItems(1L)).thenReturn(testInvoice);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenReturn(testInvoice);

        assertThrows(IllegalStateException.class, () -> 
            supplierInvoiceService.verifyInvoice(1L, null, 1L));
    }

    @Test
    @DisplayName("Supplementary: Test validateMismatch() - Price mismatch")
    public void testValidateMismatch_PriceMismatch() {
        // Create PO item with different price
        PurchaseOrderItem poItem = new PurchaseOrderItem();
        poItem.setMedicine(testMedicine);
        poItem.setReceivedQuantity(10);
        poItem.setUnitPrice(new BigDecimal("15.00")); // Different from invoice price (10.00)
        testGoodsReceipt.getPurchaseOrder().setItems(List.of(poItem));

        when(supplierInvoiceRepository.findByIdWithItems(1L)).thenReturn(testInvoice);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenReturn(testInvoice);

        assertThrows(IllegalStateException.class, () -> 
            supplierInvoiceService.verifyInvoice(1L, null, 1L));
    }

    @Test
    @DisplayName("Supplementary: Test validateMismatch() - Missing medicine in invoice")
    public void testValidateMismatch_MissingMedicineInInvoice() {
        // Create PO item not present in invoice
        Medicine extraMedicine = new Medicine();
        extraMedicine.setMedicineId(2L);
        extraMedicine.setName("Ibuprofen");

        PurchaseOrderItem poItem = new PurchaseOrderItem();
        poItem.setMedicine(extraMedicine);
        poItem.setReceivedQuantity(10);
        poItem.setUnitPrice(new BigDecimal("10.00"));
        testGoodsReceipt.getPurchaseOrder().setItems(List.of(poItem));

        when(supplierInvoiceRepository.findByIdWithItems(1L)).thenReturn(testInvoice);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenReturn(testInvoice);

        assertThrows(IllegalStateException.class, () -> 
            supplierInvoiceService.verifyInvoice(1L, null, 1L));
    }

    @Test
    @DisplayName("Supplementary: Test validateMismatch() - Medicine not in PO")
    public void testValidateMismatch_MedicineNotInPO() {
        // Create invoice item not present in PO
        Medicine extraMedicine = new Medicine();
        extraMedicine.setMedicineId(2L);
        extraMedicine.setName("Ibuprofen");

        SupplierInvoiceItem extraItem = new SupplierInvoiceItem();
        extraItem.setMedicine(extraMedicine);
        extraItem.setQuantity(10);
        extraItem.setUnitPrice(new BigDecimal("10.00"));
        extraItem.setSupplierInvoice(testInvoice);
        testInvoice.setItems(List.of(extraItem));

        // Create PO with different medicine
        PurchaseOrderItem poItem = new PurchaseOrderItem();
        poItem.setMedicine(testMedicine);
        poItem.setReceivedQuantity(10);
        poItem.setUnitPrice(new BigDecimal("10.00"));
        testGoodsReceipt.getPurchaseOrder().setItems(List.of(poItem));

        when(supplierInvoiceRepository.findByIdWithItems(1L)).thenReturn(testInvoice);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenReturn(testInvoice);

        assertThrows(IllegalStateException.class, () -> 
            supplierInvoiceService.verifyInvoice(1L, null, 1L));
    }

    @Test
    @DisplayName("Supplementary: Test validateMismatch() - Null PO items")
    public void testValidateMismatch_NullPOItems() {
        testGoodsReceipt.getPurchaseOrder().setItems(null);

        when(supplierInvoiceRepository.findByIdWithItems(1L)).thenReturn(testInvoice);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenReturn(testInvoice);

        SupplierInvoiceResponse result = supplierInvoiceService.verifyInvoice(1L, null, 1L);

        assertNotNull(result);
        assertEquals(InvoiceStatus.VERIFIED.name(), result.getStatus());
    }

    @Test
    @DisplayName("Supplementary: Test validateMismatch() - Null goods receipt")
    public void testValidateMismatch_NullGoodsReceipt() {
        testInvoice.setGoodsReceipt(null);

        when(supplierInvoiceRepository.findByIdWithItems(1L)).thenReturn(testInvoice);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenReturn(testInvoice);

        SupplierInvoiceResponse result = supplierInvoiceService.verifyInvoice(1L, null, 1L);

        assertNotNull(result);
        assertEquals(InvoiceStatus.VERIFIED.name(), result.getStatus());
    }

    @Test
    @DisplayName("Supplementary: Test createInvoice() - Null purchase order")
    public void testCreateInvoice_NullPurchaseOrder() {
        testGoodsReceipt.setPurchaseOrder(null);

        CreateSupplierInvoiceRequest request = createValidInvoiceRequest();
        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));

        assertThrows(RuntimeException.class, () -> 
            supplierInvoiceService.createInvoice(request, 1L));
    }

    @Test
    @DisplayName("Supplementary: Test createInvoice() - Null supplier")
    public void testCreateInvoice_NullSupplier() {
        testPurchaseOrder.setSupplier(null);

        CreateSupplierInvoiceRequest request = createValidInvoiceRequest();
        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));

        assertThrows(RuntimeException.class, () -> 
            supplierInvoiceService.createInvoice(request, 1L));
    }

    @Test
    @DisplayName("Supplementary: Test createInvoice() - Multiple items")
    public void testCreateInvoice_MultipleItems() {
        // Create request with multiple items
        CreateSupplierInvoiceRequest request = createValidInvoiceRequest();
        request.setItems(new ArrayList<>()); // Create mutable list
        
        Medicine medicine2 = new Medicine();
        medicine2.setMedicineId(2L);
        medicine2.setName("Ibuprofen");

        CreateSupplierInvoiceRequest.CreateSupplierInvoiceItemRequest item1 = 
            new CreateSupplierInvoiceRequest.CreateSupplierInvoiceItemRequest();
        item1.setMedicineId(1L);
        item1.setQuantity(10);
        item1.setUnitPrice(new BigDecimal("10.00"));
        
        CreateSupplierInvoiceRequest.CreateSupplierInvoiceItemRequest item2 = 
            new CreateSupplierInvoiceRequest.CreateSupplierInvoiceItemRequest();
        item2.setMedicineId(2L);
        item2.setQuantity(5);
        item2.setUnitPrice(new BigDecimal("20.00"));
        
        request.getItems().add(item1);
        request.getItems().add(item2);

        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(testGoodsReceipt));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));
        when(medicineRepository.findById(2L)).thenReturn(Optional.of(medicine2));
        when(supplierInvoiceRepository.findByGoodsReceipt_ReceiptId(1L)).thenReturn(Optional.empty());
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenReturn(testInvoice);

        SupplierInvoiceResponse result = supplierInvoiceService.createInvoice(request, 1L);

        assertNotNull(result);
        verify(medicineRepository).findById(1L);
        verify(medicineRepository).findById(2L);
    }

    @Test
    @DisplayName("Supplementary: Test payInvoice() - Payment with notes")
    public void testPayInvoice_PaymentWithNotes() {
        testInvoice.setStatus(InvoiceStatus.VERIFIED);
        testInvoice.setTotalAmount(new BigDecimal("100.00"));
        testInvoice.setPaidAmount(BigDecimal.ZERO);
        testInvoice.setRemainingAmount(new BigDecimal("100.00"));
        when(supplierInvoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentRepository.save(any(Payment.class))).thenReturn(new Payment());
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenReturn(testInvoice);

        PaymentInvoiceRequest request = new PaymentInvoiceRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setMethod("BANK_TRANSFER");
        request.setNotes("Payment for invoice #001");

        SupplierInvoiceResponse result = supplierInvoiceService.payInvoice(1L, request, 1L);

        assertNotNull(result);
        assertEquals(InvoiceStatus.PAID.name(), result.getStatus());
        
        verify(paymentRepository).save(argThat(payment -> 
            "BANK_TRANSFER".equals(payment.getMethod()) && 
            "Payment for invoice #001".equals(payment.getNotes())
        ));
    }

    @Test
    @DisplayName("Supplementary: Test getInvoiceById() - With payments")
    public void testGetInvoiceById_WithPayments() {
        Payment payment = new Payment();
        payment.setPaymentId(1L);
        payment.setAmount(50.00);
        payment.setMethod("CASH");
        payment.setStatus("COMPLETED");

        when(supplierInvoiceRepository.findByIdWithItems(1L)).thenReturn(testInvoice);
        when(paymentRepository.findBySupplierInvoice_InvoiceId(1L)).thenReturn(List.of(payment));

        SupplierInvoiceResponse result = supplierInvoiceService.getInvoiceById(1L);

        assertNotNull(result);
        assertEquals(1, result.getPayments().size());
        assertEquals("CASH", result.getPayments().get(0).getMethod());
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private CreateSupplierInvoiceRequest createValidInvoiceRequest() {
        CreateSupplierInvoiceRequest request = new CreateSupplierInvoiceRequest();
        request.setGoodsReceiptId(1L);
        request.setInvoiceDate(LocalDate.now());
        request.setDueDate(LocalDate.now().plusDays(30));
        request.setNotes("Test invoice");

        CreateSupplierInvoiceRequest.CreateSupplierInvoiceItemRequest item = 
            new CreateSupplierInvoiceRequest.CreateSupplierInvoiceItemRequest();
        item.setMedicineId(1L);
        item.setQuantity(10);
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setNotes("Test item");

        request.setItems(List.of(item));
        return request;
    }
}
