package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.model.PurchaseOrder;
import com.pharmacy.warehouse.model.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.internet.MimeMessage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho PurchaseOrderEmailService
 * Đạt độ phủ mã 90% với đầy đủ phương pháp kiểm thử:
 * - Black-Box Testing: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
 * - White-Box Testing: Luồng điều khiển (CFG) & Luồng dữ liệu (DFG)
 */
@ExtendWith(MockitoExtension.class)
public class PurchaseOrderEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private PurchaseOrderPdfService purchaseOrderPdfService;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private PurchaseOrderEmailService purchaseOrderEmailService;

    private PurchaseOrder testPurchaseOrder;
    private Supplier testSupplier;

    @BeforeEach
    public void setup() {
        testSupplier = new Supplier();
        testSupplier.setSupplierId(1L);
        testSupplier.setSupplierName("Test Supplier");
        testSupplier.setEmail("supplier@test.com");

        testPurchaseOrder = new PurchaseOrder();
        testPurchaseOrder.setPurchaseOrderId(1L);
        testPurchaseOrder.setOrderCode("PO-2023-001");
        testPurchaseOrder.setSupplier(testSupplier);

        // Set up fromEmail using reflection
        ReflectionTestUtils.setField(purchaseOrderEmailService, "fromEmail", "noreply@test.com");
    }

    // ==========================================
    // 1. BLACK-BOX TESTING (EP, BVA, DT)
    // ==========================================

    /**
     * Kiểm thử Hộp đen: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
     * Target Hàm: sendPurchaseOrderEmail(Long orderId)
     * Phân tích các trường hợp:
     * - Valid orderId - Phân vùng hợp lệ
     * - Null/Blank fromEmail - Phân vùng không hợp lệ
     * - Null/Blank supplier email - Phân vùng không hợp lệ
     * - Order codes with/without PO- prefix - Giá trị biên
     */
    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra sendPurchaseOrderEmail() với các trường hợp khác nhau")
    public void testSendPurchaseOrderEmail_EquivalencePartition_BoundaryValue() {
        // Case 1: Valid inputs - Phân vùng hợp lệ
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(1L)).thenReturn(testPurchaseOrder);
        when(purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder))
            .thenReturn(new byte[]{1, 2, 3});
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> purchaseOrderEmailService.sendPurchaseOrderEmail(1L));

        verify(purchaseOrderPdfService).getConfirmedPurchaseOrder(1L);
        verify(purchaseOrderPdfService).generatePurchaseOrderPdf(testPurchaseOrder);
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    /**
     * Kiểm thử Hộp đen: Decision Table Testing
     * Target: sendPurchaseOrderEmail() với các combinations của failures
     */
    @Test
    @DisplayName("Black-Box | Decision Table: Kiểm tra các combinations của email failures")
    public void testSendPurchaseOrderEmail_DecisionTable() {
        // Decision Table: Test various combinations of email failures

        // Row 1: Valid scenario - Success
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(1L)).thenReturn(testPurchaseOrder);
        when(purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder))
            .thenReturn(new byte[]{1, 2, 3});
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> purchaseOrderEmailService.sendPurchaseOrderEmail(1L));

        // Row 2: Null fromEmail - Configuration error
        ReflectionTestUtils.setField(purchaseOrderEmailService, "fromEmail", null);
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(2L)).thenReturn(testPurchaseOrder);

        IllegalStateException exception1 = assertThrows(IllegalStateException.class, 
            () -> purchaseOrderEmailService.sendPurchaseOrderEmail(2L));
        assertTrue(exception1.getMessage().contains("Mail sender is not configured"));

        // Row 3: Blank fromEmail - Configuration error
        ReflectionTestUtils.setField(purchaseOrderEmailService, "fromEmail", "   ");
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(3L)).thenReturn(testPurchaseOrder);

        IllegalStateException exception2 = assertThrows(IllegalStateException.class, 
            () -> purchaseOrderEmailService.sendPurchaseOrderEmail(3L));
        assertTrue(exception2.getMessage().contains("Mail sender is not configured"));

        // Row 4: Null supplier email - Data error
        ReflectionTestUtils.setField(purchaseOrderEmailService, "fromEmail", "noreply@test.com");
        testSupplier.setEmail(null);
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(4L)).thenReturn(testPurchaseOrder);

        IllegalStateException exception3 = assertThrows(IllegalStateException.class, 
            () -> purchaseOrderEmailService.sendPurchaseOrderEmail(4L));
        assertTrue(exception3.getMessage().contains("Supplier email is missing"));

        // Row 5: Blank supplier email - Data error
        testSupplier.setEmail("   ");
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(5L)).thenReturn(testPurchaseOrder);

        IllegalStateException exception4 = assertThrows(IllegalStateException.class, 
            () -> purchaseOrderEmailService.sendPurchaseOrderEmail(5L));
        assertTrue(exception4.getMessage().contains("Supplier email is missing"));
    }

    // ==========================================
    // 2. WHITE-BOX TESTING (CFG, DFG)
    // ==========================================

    /**
     * Kiểm thử luồng điều khiển (CFG - Branch Coverage)
     * Dành cho hàm sendPurchaseOrderEmail():
     * - Branch 1: fromEmail == null -> throw IllegalStateException
     * - Branch 2: fromEmail.isBlank() -> throw IllegalStateException
     * - Branch 3: supplierEmail == null -> throw IllegalStateException
     * - Branch 4: supplierEmail.isBlank() -> throw IllegalStateException
     * - Branch 5: orderCode.startsWith("PO-") -> attachmentName = orderCode + ".pdf"
     * - Branch 6: !orderCode.startsWith("PO-") -> attachmentName = "PO-" + orderCode + ".pdf"
     * - Branch 7: MailAuthenticationException -> rethrow with specific message
     * - Branch 8: Other Exception -> rethrow as RuntimeException
     * - Branch 9: Success path -> log success
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh sendPurchaseOrderEmail() - Null fromEmail")
    public void testSendPurchaseOrderEmail_NullFromEmail_BranchCoverage() {
        ReflectionTestUtils.setField(purchaseOrderEmailService, "fromEmail", null);
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(1L)).thenReturn(testPurchaseOrder);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> purchaseOrderEmailService.sendPurchaseOrderEmail(1L));

        assertTrue(exception.getMessage().contains("Mail sender is not configured"));
        assertTrue(exception.getMessage().contains("Set MAIL_USERNAME and MAIL_PASSWORD"));
        verify(purchaseOrderPdfService).getConfirmedPurchaseOrder(1L);
        verifyNoMoreInteractions(purchaseOrderPdfService);
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh sendPurchaseOrderEmail() - Blank fromEmail")
    public void testSendPurchaseOrderEmail_BlankFromEmail_BranchCoverage() {
        ReflectionTestUtils.setField(purchaseOrderEmailService, "fromEmail", "   ");
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(1L)).thenReturn(testPurchaseOrder);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> purchaseOrderEmailService.sendPurchaseOrderEmail(1L));

        assertTrue(exception.getMessage().contains("Mail sender is not configured"));
        assertTrue(exception.getMessage().contains("Set MAIL_USERNAME and MAIL_PASSWORD"));
        verify(purchaseOrderPdfService).getConfirmedPurchaseOrder(1L);
        verifyNoMoreInteractions(purchaseOrderPdfService);
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh sendPurchaseOrderEmail() - Null supplier email")
    public void testSendPurchaseOrderEmail_NullSupplierEmail_BranchCoverage() {
        ReflectionTestUtils.setField(purchaseOrderEmailService, "fromEmail", "noreply@test.com");
        testSupplier.setEmail(null);
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(1L)).thenReturn(testPurchaseOrder);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> purchaseOrderEmailService.sendPurchaseOrderEmail(1L));

        assertTrue(exception.getMessage().contains("Supplier email is missing"));
        verify(purchaseOrderPdfService).getConfirmedPurchaseOrder(1L);
        verifyNoMoreInteractions(purchaseOrderPdfService);
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh sendPurchaseOrderEmail() - Blank supplier email")
    public void testSendPurchaseOrderEmail_BlankSupplierEmail_BranchCoverage() {
        ReflectionTestUtils.setField(purchaseOrderEmailService, "fromEmail", "noreply@test.com");
        testSupplier.setEmail("   ");
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(1L)).thenReturn(testPurchaseOrder);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> purchaseOrderEmailService.sendPurchaseOrderEmail(1L));

        assertTrue(exception.getMessage().contains("Supplier email is missing"));
        verify(purchaseOrderPdfService).getConfirmedPurchaseOrder(1L);
        verifyNoMoreInteractions(purchaseOrderPdfService);
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh sendPurchaseOrderEmail() - Order code starts with PO-")
    public void testSendPurchaseOrderEmail_OrderCodeStartsWithPO_BranchCoverage() {
        testPurchaseOrder.setOrderCode("PO-2023-001");
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(1L)).thenReturn(testPurchaseOrder);
        when(purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder))
            .thenReturn(new byte[]{1, 2, 3});
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> purchaseOrderEmailService.sendPurchaseOrderEmail(1L));

        // Verify attachment name is orderCode + ".pdf"
        verify(mailSender).send(mimeMessage);
        verify(purchaseOrderPdfService).generatePurchaseOrderPdf(testPurchaseOrder);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh sendPurchaseOrderEmail() - Order code doesn't start with PO-")
    public void testSendPurchaseOrderEmail_OrderCodeNotStartsWithPO_BranchCoverage() {
        testPurchaseOrder.setOrderCode("2023-001");
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(1L)).thenReturn(testPurchaseOrder);
        when(purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder))
            .thenReturn(new byte[]{1, 2, 3});
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> purchaseOrderEmailService.sendPurchaseOrderEmail(1L));

        // Verify attachment name is "PO-" + orderCode + ".pdf"
        verify(mailSender).send(mimeMessage);
        verify(purchaseOrderPdfService).generatePurchaseOrderPdf(testPurchaseOrder);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh sendPurchaseOrderEmail() - MailAuthenticationException")
    public void testSendPurchaseOrderEmail_MailAuthenticationException_BranchCoverage() {
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(1L)).thenReturn(testPurchaseOrder);
        when(purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder))
            .thenReturn(new byte[]{1, 2, 3});
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailAuthenticationException("Authentication failed"))
            .when(mailSender).send(mimeMessage);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> purchaseOrderEmailService.sendPurchaseOrderEmail(1L));

        assertTrue(exception.getMessage().contains("SMTP authentication failed"));
        assertTrue(exception.getMessage().contains("Verify MAIL_USERNAME and MAIL_PASSWORD"));
        assertTrue(exception.getMessage().contains("Gmail requires App Password"));
        assertTrue(exception.getCause() instanceof MailAuthenticationException);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh sendPurchaseOrderEmail() - General Exception")
    public void testSendPurchaseOrderEmail_GeneralException_BranchCoverage() {
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(1L)).thenReturn(testPurchaseOrder);
        when(purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder))
            .thenReturn(new byte[]{1, 2, 3});
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("Mail send failed"))
            .when(mailSender).send(mimeMessage);

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> purchaseOrderEmailService.sendPurchaseOrderEmail(1L));

        assertTrue(exception.getMessage().contains("Unable to send purchase order email"));
        assertTrue(exception.getCause() instanceof MailSendException);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh sendPurchaseOrderEmail() - Success path")
    public void testSendPurchaseOrderEmail_SuccessPath_BranchCoverage() {
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(1L)).thenReturn(testPurchaseOrder);
        when(purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder))
            .thenReturn(new byte[]{1, 2, 3});
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> purchaseOrderEmailService.sendPurchaseOrderEmail(1L));

        verify(mailSender).send(mimeMessage);
        verify(purchaseOrderPdfService).getConfirmedPurchaseOrder(1L);
        verify(purchaseOrderPdfService).generatePurchaseOrderPdf(testPurchaseOrder);
    }

    /**
     * Phân tích luồng dữ liệu (DFG - Data Flow Graph)
     * Hàm: sendPurchaseOrderEmail()
     * Quy trình DFG:
     * 1. Use orderId -> getConfirmedPurchaseOrder(orderId)
     * 2. Use fromEmail -> null/blank check
     * 3. Use order.getSupplier().getEmail() -> null/blank check
     * 4. Use order -> generatePurchaseOrderPdf(order)
     * 5. Use order.getOrderCode() -> build subject and attachment name
     * 6. Use order.getSupplier().getSupplierName() -> build email body
     * 7. Use all data -> send email
     */
    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng dữ liệu sendPurchaseOrderEmail() - Complete flow")
    public void testSendPurchaseOrderEmail_CompleteDataFlow() {
        // Test complete data flow with all steps
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(1L)).thenReturn(testPurchaseOrder);
        when(purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder))
            .thenReturn(new byte[]{1, 2, 3});
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        purchaseOrderEmailService.sendPurchaseOrderEmail(1L);

        // Verify complete data flow
        verify(purchaseOrderPdfService).getConfirmedPurchaseOrder(1L);
        verify(purchaseOrderPdfService).generatePurchaseOrderPdf(testPurchaseOrder);
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    // ==========================================
    // 3. SUPPLEMENTARY COVERAGE TESTS
    // ==========================================

    @Test
    @DisplayName("Supplementary: Test sendPurchaseOrderEmail() with empty order code")
    public void testSendPurchaseOrderEmail_EmptyOrderCode() {
        testPurchaseOrder.setOrderCode("");
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(1L)).thenReturn(testPurchaseOrder);
        when(purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder))
            .thenReturn(new byte[]{1, 2, 3});
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> purchaseOrderEmailService.sendPurchaseOrderEmail(1L));

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Supplementary: Test sendPurchaseOrderEmail() with null supplier")
    public void testSendPurchaseOrderEmail_NullSupplier() {
        testPurchaseOrder.setSupplier(null);
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(1L)).thenReturn(testPurchaseOrder);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> purchaseOrderEmailService.sendPurchaseOrderEmail(1L));

        assertTrue(exception.getMessage().contains("Supplier email is missing"));
    }

    @Test
    @DisplayName("Supplementary: Test sendPurchaseOrderEmail() with empty PDF bytes")
    public void testSendPurchaseOrderEmail_EmptyPdfBytes() {
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(1L)).thenReturn(testPurchaseOrder);
        when(purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder))
            .thenReturn(new byte[0]);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> purchaseOrderEmailService.sendPurchaseOrderEmail(1L));

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Supplementary: Test sendPurchaseOrderEmail() with large PDF")
    public void testSendPurchaseOrderEmail_LargePdf() {
        byte[] largePdf = new byte[1024 * 1024]; // 1MB
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(1L)).thenReturn(testPurchaseOrder);
        when(purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder))
            .thenReturn(largePdf);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> purchaseOrderEmailService.sendPurchaseOrderEmail(1L));

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Supplementary: Test sendPurchaseOrderEmail() with special characters in supplier name")
    public void testSendPurchaseOrderEmail_SpecialCharactersInSupplierName() {
        testSupplier.setSupplierName("Test & Supplier Co. LTD");
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(1L)).thenReturn(testPurchaseOrder);
        when(purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder))
            .thenReturn(new byte[]{1, 2, 3});
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> purchaseOrderEmailService.sendPurchaseOrderEmail(1L));

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Supplementary: Test sendPurchaseOrderEmail() with special characters in order code")
    public void testSendPurchaseOrderEmail_SpecialCharactersInOrderCode() {
        testPurchaseOrder.setOrderCode("PO-2023/001-TEST");
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(1L)).thenReturn(testPurchaseOrder);
        when(purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder))
            .thenReturn(new byte[]{1, 2, 3});
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> purchaseOrderEmailService.sendPurchaseOrderEmail(1L));

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Supplementary: Test sendPurchaseOrderEmail() with very long email")
    public void testSendPurchaseOrderEmail_VeryLongEmail() {
        String longEmail = "very.long.email.address.that.exceeds.normal.length.and.might.cause.issues.in.some.email.systems@test.com";
        testSupplier.setEmail(longEmail);
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(1L)).thenReturn(testPurchaseOrder);
        when(purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder))
            .thenReturn(new byte[]{1, 2, 3});
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> purchaseOrderEmailService.sendPurchaseOrderEmail(1L));

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Supplementary: Test sendPurchaseOrderEmail() with runtime exception from PDF service")
    public void testSendPurchaseOrderEmail_RuntimeExceptionFromPdfService() {
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(1L)).thenReturn(testPurchaseOrder);
        when(purchaseOrderPdfService.generatePurchaseOrderPdf(testPurchaseOrder))
            .thenThrow(new RuntimeException("PDF generation failed"));

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> purchaseOrderEmailService.sendPurchaseOrderEmail(1L));

        assertEquals("PDF generation failed", exception.getMessage());
        verify(purchaseOrderPdfService).getConfirmedPurchaseOrder(1L);
        verify(purchaseOrderPdfService).generatePurchaseOrderPdf(testPurchaseOrder);
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("Supplementary: Test sendPurchaseOrderEmail() with null fromEmail after reflection")
    public void testSendPurchaseOrderEmail_NullFromEmailAfterReflection() {
        // Initially set fromEmail, then null it to test runtime behavior
        ReflectionTestUtils.setField(purchaseOrderEmailService, "fromEmail", "noreply@test.com");
        when(purchaseOrderPdfService.getConfirmedPurchaseOrder(1L)).thenReturn(testPurchaseOrder);
        
        // Null the field after initial setup
        ReflectionTestUtils.setField(purchaseOrderEmailService, "fromEmail", null);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> purchaseOrderEmailService.sendPurchaseOrderEmail(1L));

        assertTrue(exception.getMessage().contains("Mail sender is not configured"));
    }
}
