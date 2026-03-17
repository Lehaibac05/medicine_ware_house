package com.pharmacy.warehouse.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pharmacy.warehouse.model.PurchaseOrder;
import com.pharmacy.warehouse.model.PurchaseOrder.PurchaseOrderStatus;
import com.pharmacy.warehouse.model.PurchaseOrderItem;
import com.pharmacy.warehouse.repository.PurchaseOrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderPdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PurchaseOrderRepository purchaseOrderRepository;

    @Transactional(readOnly = true)
    public byte[] generatePurchaseOrderPdf(Long orderId) {
        PurchaseOrder order = getConfirmedPurchaseOrder(orderId);
        return generatePurchaseOrderPdf(order);
    }

    public byte[] generatePurchaseOrderPdf(PurchaseOrder order) {
        log.info("Generating PDF for purchase order: {}", order.getOrderCode());

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);

            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph title = new Paragraph("PURCHASE ORDER", titleFont);
            title.setSpacingAfter(12f);
            document.add(title);

            document.add(new Paragraph("Purchase Order Code: " + safe(order.getOrderCode()), bodyFont));
            document.add(new Paragraph("Supplier Name: " + safe(order.getSupplier().getSupplierName()), bodyFont));
            document.add(new Paragraph("Supplier Email: " + safe(order.getSupplier().getEmail()), bodyFont));
            document.add(new Paragraph("Warehouse: " + safe(order.getWarehouse().getName()), bodyFont));
            document.add(new Paragraph("Expected Delivery Date: " + formatDate(order.getExpectedDeliveryDate()), bodyFont));
            document.add(new Paragraph("Created By: " + safe(order.getCreatedBy().getFullName()), bodyFont));
            document.add(new Paragraph("Created Date: " + formatDateTime(order.getCreatedAt()), bodyFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100f);
            table.setWidths(new float[] {4f, 1.5f, 2f, 2f});

            addHeaderCell(table, "Medicine", headerFont);
            addHeaderCell(table, "Quantity", headerFont);
            addHeaderCell(table, "Unit Price", headerFont);
            addHeaderCell(table, "Total", headerFont);

            if (order.getItems() != null) {
                for (PurchaseOrderItem item : order.getItems()) {
                    addBodyCell(table, safe(item.getMedicine() != null ? item.getMedicine().getName() : null), bodyFont);
                    addBodyCell(table, String.valueOf(item.getRequestedQuantity() != null ? item.getRequestedQuantity() : 0), bodyFont);
                    addBodyCell(table, formatMoney(item.getUnitPrice()), bodyFont);
                    addBodyCell(table, formatMoney(item.getTotalPrice()), bodyFont);
                }
            }

            document.add(table);
            document.add(new Paragraph(" "));

            Paragraph total = new Paragraph("Total Amount: " + formatMoney(order.getTotalAmount()), headerFont);
            total.setAlignment(Paragraph.ALIGN_RIGHT);
            document.add(total);

            document.close();

            log.info("Generated PDF successfully for purchase order: {}", order.getOrderCode());
            return outputStream.toByteArray();
        } catch (Exception ex) {
            log.error("Failed to generate PDF for order {}: {}", order.getPurchaseOrderId(), ex.getMessage(), ex);
            throw new RuntimeException("Unable to generate purchase order PDF", ex);
        }
    }

    @Transactional(readOnly = true)
    public PurchaseOrder getConfirmedPurchaseOrder(Long orderId) {
        PurchaseOrder order = purchaseOrderRepository.findByIdWithItems(orderId);
        if (order == null) {
            throw new RuntimeException("Purchase order not found with id: " + orderId);
        }

        if (order.getStatus() != PurchaseOrderStatus.CONFIRMED) {
            throw new IllegalStateException("Purchase order must be CONFIRMED");
        }

        return order;
    }

    private void addHeaderCell(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setBorder(Rectangle.BOX);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setBorder(Rectangle.BOX);
        table.addCell(cell);
    }

    private String formatDate(java.time.LocalDate date) {
        return date != null ? DATE_FORMAT.format(date) : "-";
    }

    private String formatDateTime(java.time.LocalDateTime dateTime) {
        return dateTime != null ? DATETIME_FORMAT.format(dateTime) : "-";
    }

    private String formatMoney(BigDecimal value) {
        return value != null ? value.stripTrailingZeros().toPlainString() : "0";
    }

    private String safe(String value) {
        return value != null && !value.isBlank() ? value : "-";
    }
}
