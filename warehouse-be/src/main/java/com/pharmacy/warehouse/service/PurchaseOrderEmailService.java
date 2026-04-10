package com.pharmacy.warehouse.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pharmacy.warehouse.model.PurchaseOrder;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderEmailService {

    private final JavaMailSender mailSender;
    private final PurchaseOrderPdfService purchaseOrderPdfService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Transactional
    public void sendPurchaseOrderEmail(Long orderId) {
        PurchaseOrder order = purchaseOrderPdfService.getExportablePurchaseOrder(orderId);

        if (fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalStateException("Mail sender is not configured. Set MAIL_USERNAME and MAIL_PASSWORD");
        }

        String supplierEmail = order.getSupplier() != null ? order.getSupplier().getEmail() : null;
        if (supplierEmail == null || supplierEmail.isBlank()) {
            throw new IllegalStateException("Supplier email is missing");
        }

        byte[] pdfBytes = purchaseOrderPdfService.generatePurchaseOrderPdf(order);
        String subject = "Purchase Order " + order.getOrderCode();
        String body = String.format(
            "Dear %s,%n%nPlease find the attached purchase order.%n%nBest regards%nWarehouse Management System",
            order.getSupplier().getSupplierName()
        );
        String attachmentName = order.getOrderCode().startsWith("PO-")
            ? order.getOrderCode() + ".pdf"
            : "PO-" + order.getOrderCode() + ".pdf";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(supplierEmail);
            helper.setSubject(subject);
            helper.setText(body);
            helper.addAttachment(attachmentName, new ByteArrayResource(pdfBytes));

            mailSender.send(message);
            log.info("Purchase order email sent successfully. Order: {}, To: {}", order.getOrderCode(), supplierEmail);
        } catch (MailAuthenticationException ex) {
            log.error("SMTP authentication failed while sending PO email. From: {}, To: {}", fromEmail, supplierEmail, ex);
            throw new IllegalStateException(
                "SMTP authentication failed. Verify MAIL_USERNAME and MAIL_PASSWORD (Gmail requires App Password)",
                ex
            );
        } catch (Exception ex) {
            log.error("Failed to send purchase order email. Order: {}, To: {}, Error: {}",
                order.getOrderCode(), supplierEmail, ex.getMessage(), ex);
            throw new RuntimeException("Unable to send purchase order email", ex);
        }
    }
}
