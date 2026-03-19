package com.pharmacy.warehouse.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pharmacy.warehouse.dto.DashboardSummaryResponse;
import com.pharmacy.warehouse.dto.FinancialReportResponse;
import com.pharmacy.warehouse.dto.InventoryReportResponse;
import com.pharmacy.warehouse.model.Batch;
import com.pharmacy.warehouse.model.Payment;
import com.pharmacy.warehouse.model.Payment.PaymentStatus;
import com.pharmacy.warehouse.model.SupplierInvoice;
import com.pharmacy.warehouse.model.SupplierInvoice.InvoiceStatus;
import com.pharmacy.warehouse.repository.BatchRepository;
import com.pharmacy.warehouse.repository.MedicineRepository;
import com.pharmacy.warehouse.repository.PaymentRepository;
import com.pharmacy.warehouse.repository.SupplierInvoiceRepository;
import com.pharmacy.warehouse.repository.WarehouseRepository;
import com.pharmacy.warehouse.repository.projection.InventoryAggregateProjection;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int EXPIRING_SOON_DAYS = 30;

    private final BatchRepository batchRepository;
    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final PaymentRepository paymentRepository;
    private final WarehouseRepository warehouseRepository;
    private final MedicineRepository medicineRepository;

    @Transactional(readOnly = true)
    public InventoryReportResponse getInventoryReport(
            Long warehouseId,
            String medicineName,
            String medicineGroup,
            String status,
            LocalDate expiryFrom,
            LocalDate expiryTo,
            int page,
            int size) {

        validatePageAndSize(page, size);
        validateDateRange(expiryFrom, expiryTo, "Expiry date range is invalid");
        validateWarehouse(warehouseId);

        List<InventoryAggregateProjection> aggregates = batchRepository.aggregateInventory(
                medicineName,
                medicineGroup,
                warehouseId,
                expiryFrom,
                expiryTo);

        LocalDate expiringThreshold = LocalDate.now().plusDays(EXPIRING_SOON_DAYS);

        List<InventoryReportResponse.InventoryReportItem> filtered = aggregates.stream()
                .map(aggregate -> toInventoryItem(aggregate, expiringThreshold))
                .filter(item -> status == null || status.isBlank()
                        || "ALL".equalsIgnoreCase(status)
                        || item.getStatus().equalsIgnoreCase(status))
                .toList();

        return buildInventoryResponse(filtered, page, size);
    }

    @Transactional(readOnly = true)
    public InventoryReportResponse exportInventoryReport(
            Long warehouseId,
            String medicineName,
            String medicineGroup,
            String status,
            LocalDate expiryFrom,
            LocalDate expiryTo) {

        validateDateRange(expiryFrom, expiryTo, "Expiry date range is invalid");
        validateWarehouse(warehouseId);

        List<InventoryAggregateProjection> aggregates = batchRepository.aggregateInventory(
                medicineName,
                medicineGroup,
                warehouseId,
                expiryFrom,
                expiryTo);

        LocalDate expiringThreshold = LocalDate.now().plusDays(EXPIRING_SOON_DAYS);
        List<InventoryReportResponse.InventoryReportItem> items = aggregates.stream()
                .map(aggregate -> toInventoryItem(aggregate, expiringThreshold))
                .filter(item -> status == null || status.isBlank()
                        || "ALL".equalsIgnoreCase(status)
                        || item.getStatus().equalsIgnoreCase(status))
                .toList();

        return InventoryReportResponse.builder()
                .items(items)
                .page(0)
                .size(items.size())
                .totalElements(items.size())
                .totalPages(items.isEmpty() ? 0 : 1)
                .generatedAt(LocalDateTime.now())
                .exportColumns(List.of(
                        "medicineId",
                        "medicineName",
                        "warehouseId",
                        "warehouseName",
                        "totalStock",
                        "batchCount",
                        "nearestExpiryDate",
                        "status",
                        "reorderLevel"))
                .build();
    }

    @Transactional(readOnly = true)
    public FinancialReportResponse getFinancialReport(
            LocalDate fromDate,
            LocalDate toDate,
            Long supplierId,
            String status,
            int page,
            int size) {

        validatePageAndSize(page, size);
        validateDateRange(fromDate, toDate, "Financial date range is invalid");

        List<FinancialReportResponse.FinancialReportItem> items = buildFinancialItems(fromDate, toDate, supplierId, status);
        return buildFinancialResponse(items, page, size);
    }

    @Transactional(readOnly = true)
    public FinancialReportResponse exportFinancialReport(
            LocalDate fromDate,
            LocalDate toDate,
            Long supplierId,
            String status) {

        validateDateRange(fromDate, toDate, "Financial date range is invalid");

        List<FinancialReportResponse.FinancialReportItem> items = buildFinancialItems(fromDate, toDate, supplierId, status);
        BigDecimal totalInvoice = items.stream()
                .map(FinancialReportResponse.FinancialReportItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid = items.stream()
                .map(FinancialReportResponse.FinancialReportItem::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRemaining = items.stream()
                .map(FinancialReportResponse.FinancialReportItem::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return FinancialReportResponse.builder()
                .items(items)
                .page(0)
                .size(items.size())
                .totalElements(items.size())
                .totalPages(items.isEmpty() ? 0 : 1)
                .totalInvoiceAmount(totalInvoice)
                .totalPaidAmount(totalPaid)
                .totalRemainingAmount(totalRemaining)
                .generatedAt(LocalDateTime.now())
                .exportColumns(List.of(
                        "invoiceId",
                        "invoiceCode",
                        "supplierId",
                        "supplierName",
                        "invoiceDate",
                        "dueDate",
                        "totalAmount",
                        "paidAmount",
                        "remainingAmount",
                        "status"))
                .build();
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {
        List<InventoryAggregateProjection> inventory = batchRepository.aggregateInventory(null, null, null, null, null);
        List<SupplierInvoice> invoices = supplierInvoiceRepository.findAll();

        long totalMedicines = medicineRepository.count();
        long totalStock = inventory.stream().mapToLong(item -> safeLong(item.getTotalStock())).sum();

        LocalDate expiringThreshold = LocalDate.now().plusDays(EXPIRING_SOON_DAYS);
        List<InventoryReportResponse.InventoryReportItem> inventoryItems = inventory.stream()
                .map(item -> toInventoryItem(item, expiringThreshold))
                .toList();

        long lowStockCount = inventoryItems.stream().filter(item -> "LOW_STOCK".equals(item.getStatus())).count();
        long expiringSoonCount = inventoryItems.stream().filter(item -> "EXPIRING_SOON".equals(item.getStatus())).count();
        long totalInvoices = invoices.size();

        BigDecimal totalPaid = invoices.stream()
                .map(invoice -> safeAmount(invoice.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalUnpaid = invoices.stream()
                .map(invoice -> safeAmount(invoice.getRemainingAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DashboardSummaryResponse.WarehouseStockPoint> stockByWarehouse = inventory.stream()
                .collect(Collectors.groupingBy(
                        InventoryAggregateProjection::getWarehouseId,
                        Collectors.collectingAndThen(Collectors.toList(), rows -> {
                            InventoryAggregateProjection first = rows.get(0);
                            long stock = rows.stream().mapToLong(row -> safeLong(row.getTotalStock())).sum();
                            return DashboardSummaryResponse.WarehouseStockPoint.builder()
                                    .warehouseId(first.getWarehouseId())
                                    .warehouseName(first.getWarehouseName())
                                    .totalStock(stock)
                                    .build();
                        })))
                .values().stream()
                .sorted(Comparator.comparing(DashboardSummaryResponse.WarehouseStockPoint::getWarehouseName))
                .toList();

        long paidInvoices = invoices.stream().filter(invoice -> invoice.getStatus() == InvoiceStatus.PAID).count();
        long unpaidInvoices = invoices.stream().filter(invoice -> invoice.getStatus() != InvoiceStatus.PAID).count();
        List<DashboardSummaryResponse.InvoiceStatusPoint> invoiceStatusDistribution = List.of(
                DashboardSummaryResponse.InvoiceStatusPoint.builder().status("PAID").value(paidInvoices).build(),
                DashboardSummaryResponse.InvoiceStatusPoint.builder().status("UNPAID").value(unpaidInvoices).build());

        List<DashboardSummaryResponse.MonthlySpendingPoint> monthlySpending = buildMonthlySpending();

        List<DashboardSummaryResponse.LowStockAlertItem> lowStockItems = inventoryItems.stream()
                .filter(item -> "LOW_STOCK".equals(item.getStatus()))
                .sorted(Comparator.comparing(InventoryReportResponse.InventoryReportItem::getTotalStock))
                .limit(10)
                .map(item -> DashboardSummaryResponse.LowStockAlertItem.builder()
                        .medicineId(item.getMedicineId())
                        .medicineName(item.getMedicineName())
                        .warehouseId(item.getWarehouseId())
                        .warehouseName(item.getWarehouseName())
                        .totalStock(item.getTotalStock())
                        .reorderLevel(item.getReorderLevel())
                        .build())
                .toList();

        List<DashboardSummaryResponse.ExpiringBatchAlertItem> expiringBatchItems = batchRepository
                .findExpiringBatches(expiringThreshold).stream()
                .limit(10)
                .map(batch -> DashboardSummaryResponse.ExpiringBatchAlertItem.builder()
                        .batchId(batch.getBatchId())
                        .lotNumber(batch.getLotNumber())
                        .medicineId(batch.getMedicine() != null ? batch.getMedicine().getMedicineId() : null)
                        .medicineName(batch.getMedicine() != null ? batch.getMedicine().getName() : null)
                        .warehouseId(batch.getWarehouse() != null ? batch.getWarehouse().getWarehouseId() : null)
                        .warehouseName(batch.getWarehouse() != null ? batch.getWarehouse().getName() : null)
                        .expiryDate(batch.getExpiryDate())
                        .quantity(batch.getQuantity())
                        .build())
                .toList();

        return DashboardSummaryResponse.builder()
                .totalMedicines(totalMedicines)
                .totalStock(totalStock)
                .lowStockCount(lowStockCount)
                .expiringSoonCount(expiringSoonCount)
                .totalInvoices(totalInvoices)
                .totalPaid(totalPaid)
                .totalUnpaid(totalUnpaid)
                .stockByWarehouse(stockByWarehouse)
                .invoiceStatusDistribution(invoiceStatusDistribution)
                .monthlySpending(monthlySpending)
                .lowStockItems(lowStockItems)
                .expiringBatchItems(expiringBatchItems)
                .build();
    }

    private List<DashboardSummaryResponse.MonthlySpendingPoint> buildMonthlySpending() {
        List<Payment> payments = paymentRepository.findAll().stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.COMPLETED)
                .filter(payment -> payment.getPaymentDate() != null)
                .toList();

        YearMonth currentMonth = YearMonth.now();
        Map<YearMonth, BigDecimal> points = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            points.put(currentMonth.minusMonths(i), BigDecimal.ZERO);
        }

        for (Payment payment : payments) {
            YearMonth month = YearMonth.from(payment.getPaymentDate());
            if (points.containsKey(month)) {
                points.put(month, points.get(month).add(safeAmount(payment.getAmount())));
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
        return points.entrySet().stream()
                .map(entry -> DashboardSummaryResponse.MonthlySpendingPoint.builder()
                        .month(entry.getKey().atDay(1).format(formatter))
                        .amount(entry.getValue())
                        .build())
                .toList();
    }

    private List<FinancialReportResponse.FinancialReportItem> buildFinancialItems(
            LocalDate fromDate,
            LocalDate toDate,
            Long supplierId,
            String status) {

        String normalizedStatus = status == null ? null : status.trim().toUpperCase();

        return supplierInvoiceRepository.findAll().stream()
                .filter(invoice -> fromDate == null || (invoice.getInvoiceDate() != null && !invoice.getInvoiceDate().isBefore(fromDate)))
                .filter(invoice -> toDate == null || (invoice.getInvoiceDate() != null && !invoice.getInvoiceDate().isAfter(toDate)))
                .filter(invoice -> supplierId == null
                        || (invoice.getSupplier() != null && Objects.equals(invoice.getSupplier().getSupplierId(), supplierId)))
                .map(invoice -> {
                    BigDecimal paidAmount = safeAmount(invoice.getPaidAmount());
                    BigDecimal totalAmount = safeAmount(invoice.getTotalAmount());
                    BigDecimal remaining = totalAmount.subtract(paidAmount);

                    String financialStatus;
                    if (remaining.compareTo(BigDecimal.ZERO) == 0) {
                        financialStatus = "PAID";
                    } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
                        financialStatus = "PARTIAL";
                    } else {
                        financialStatus = "UNPAID";
                    }

                    return FinancialReportResponse.FinancialReportItem.builder()
                            .invoiceId(invoice.getInvoiceId())
                            .invoiceCode(invoice.getInvoiceCode())
                            .supplierId(invoice.getSupplier() != null ? invoice.getSupplier().getSupplierId() : null)
                            .supplierName(invoice.getSupplier() != null ? invoice.getSupplier().getSupplierName() : null)
                            .invoiceDate(invoice.getInvoiceDate())
                            .dueDate(invoice.getDueDate())
                            .totalAmount(totalAmount)
                            .paidAmount(paidAmount)
                            .remainingAmount(remaining)
                            .status(financialStatus)
                            .build();
                })
                .filter(item -> normalizedStatus == null || normalizedStatus.isBlank()
                        || "ALL".equals(normalizedStatus)
                        || normalizedStatus.equals(item.getStatus()))
                .sorted(Comparator
                        .comparing(FinancialReportResponse.FinancialReportItem::getInvoiceDate,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(FinancialReportResponse.FinancialReportItem::getInvoiceCode,
                                Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private FinancialReportResponse buildFinancialResponse(
            List<FinancialReportResponse.FinancialReportItem> items,
            int page,
            int size) {

        int fromIndex = Math.min(page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        List<FinancialReportResponse.FinancialReportItem> content = items.subList(fromIndex, toIndex);

        BigDecimal totalInvoice = items.stream()
                .map(FinancialReportResponse.FinancialReportItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid = items.stream()
                .map(FinancialReportResponse.FinancialReportItem::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRemaining = items.stream()
                .map(FinancialReportResponse.FinancialReportItem::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return FinancialReportResponse.builder()
                .items(new ArrayList<>(content))
                .page(page)
                .size(size)
                .totalElements(items.size())
                .totalPages(items.isEmpty() ? 0 : (int) Math.ceil((double) items.size() / size))
                .totalInvoiceAmount(totalInvoice)
                .totalPaidAmount(totalPaid)
                .totalRemainingAmount(totalRemaining)
                .generatedAt(LocalDateTime.now())
                .exportColumns(List.of(
                        "invoiceId",
                        "invoiceCode",
                        "supplierId",
                        "supplierName",
                        "invoiceDate",
                        "dueDate",
                        "totalAmount",
                        "paidAmount",
                        "remainingAmount",
                        "status"))
                .build();
    }

    private InventoryReportResponse buildInventoryResponse(
            List<InventoryReportResponse.InventoryReportItem> items,
            int page,
            int size) {

        int fromIndex = Math.min(page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        List<InventoryReportResponse.InventoryReportItem> content = items.subList(fromIndex, toIndex);

        return InventoryReportResponse.builder()
                .items(new ArrayList<>(content))
                .page(page)
                .size(size)
                .totalElements(items.size())
                .totalPages(items.isEmpty() ? 0 : (int) Math.ceil((double) items.size() / size))
                .generatedAt(LocalDateTime.now())
                .exportColumns(List.of(
                        "medicineId",
                        "medicineName",
                        "warehouseId",
                        "warehouseName",
                        "totalStock",
                        "batchCount",
                        "nearestExpiryDate",
                        "status",
                        "reorderLevel"))
                .build();
    }

    private InventoryReportResponse.InventoryReportItem toInventoryItem(
            InventoryAggregateProjection aggregate,
            LocalDate expiringThreshold) {

        long totalStock = safeLong(aggregate.getTotalStock());
        int reorderLevel = aggregate.getReorderLevel() != null ? aggregate.getReorderLevel() : 0;
        LocalDate nearestExpiry = aggregate.getNearestExpiryDate();

        String status;
        if (totalStock < reorderLevel) {
            status = "LOW_STOCK";
        } else if (nearestExpiry != null && !nearestExpiry.isAfter(expiringThreshold)) {
            status = "EXPIRING_SOON";
        } else {
            status = "NORMAL";
        }

        return InventoryReportResponse.InventoryReportItem.builder()
                .medicineId(aggregate.getMedicineId())
                .medicineName(aggregate.getMedicineName())
                .warehouseId(aggregate.getWarehouseId())
                .warehouseName(aggregate.getWarehouseName())
                .totalStock(totalStock)
                .batchCount(safeLong(aggregate.getBatchCount()))
                .nearestExpiryDate(nearestExpiry)
                .status(status)
                .reorderLevel(reorderLevel)
                .build();
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private long safeLong(Long value) {
        return value != null ? value : 0L;
    }

    private void validatePageAndSize(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be >= 0");
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate, String message) {
        if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateWarehouse(Long warehouseId) {
        if (warehouseId != null && !warehouseRepository.existsById(warehouseId)) {
            throw new IllegalArgumentException("Warehouse not found");
        }
    }
}
