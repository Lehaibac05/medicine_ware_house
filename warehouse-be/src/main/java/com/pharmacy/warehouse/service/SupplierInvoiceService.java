package com.pharmacy.warehouse.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pharmacy.warehouse.dto.CreateSupplierInvoiceRequest;
import com.pharmacy.warehouse.dto.PaymentInvoiceRequest;
import com.pharmacy.warehouse.dto.RejectInvoiceRequest;
import com.pharmacy.warehouse.dto.SupplierInvoiceResponse;
import com.pharmacy.warehouse.dto.SupplierResponse;
import com.pharmacy.warehouse.dto.VerifyInvoiceRequest;
import com.pharmacy.warehouse.model.GoodsReceipt;
import com.pharmacy.warehouse.model.GoodsReceipt.ReceiptStatus;
import com.pharmacy.warehouse.model.Medicine;
import com.pharmacy.warehouse.model.Payment;
import com.pharmacy.warehouse.model.Payment.PaymentStatus;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierInvoiceService {

    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final MedicineRepository medicineRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    @Transactional
    public SupplierInvoiceResponse createInvoice(CreateSupplierInvoiceRequest request, Long userId) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Invoice must contain at least one item");
        }

        GoodsReceipt goodsReceipt = goodsReceiptRepository.findById(request.getGoodsReceiptId())
                .orElseThrow(() -> new RuntimeException("Goods receipt not found"));

        if (goodsReceipt.getStatus() != ReceiptStatus.APPROVED) {
            throw new IllegalStateException("Invoice can only be created for APPROVED goods receipt");
        }

        if (goodsReceipt.getPurchaseOrder() == null || goodsReceipt.getPurchaseOrder().getSupplier() == null) {
            throw new RuntimeException("Goods receipt must be linked to purchase order and supplier");
        }

        supplierInvoiceRepository.findByGoodsReceipt_ReceiptId(goodsReceipt.getReceiptId())
                .ifPresent(existing -> {
                    throw new RuntimeException("Invoice already exists for this goods receipt");
                });

        User createdBy = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Supplier supplier = goodsReceipt.getPurchaseOrder().getSupplier();

        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setGoodsReceipt(goodsReceipt);
        invoice.setSupplier(supplier);
        invoice.setCreatedBy(createdBy);
        invoice.setStatus(InvoiceStatus.PENDING_VERIFICATION);
        invoice.setInvoiceDate(request.getInvoiceDate());
        invoice.setDueDate(request.getDueDate());
        invoice.setNotes(request.getNotes());

        List<SupplierInvoiceItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CreateSupplierInvoiceRequest.CreateSupplierInvoiceItemRequest itemRequest : request.getItems()) {
            if (itemRequest.getQuantity() == null || itemRequest.getQuantity() <= 0) {
                throw new IllegalArgumentException("Item quantity must be greater than 0");
            }
            if (itemRequest.getUnitPrice() == null || itemRequest.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Item unit price is invalid");
            }

            Medicine medicine = medicineRepository.findById(itemRequest.getMedicineId())
                    .orElseThrow(() -> new RuntimeException("Medicine not found with id: " + itemRequest.getMedicineId()));

            SupplierInvoiceItem item = new SupplierInvoiceItem();
            item.setSupplierInvoice(invoice);
            item.setMedicine(medicine);
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(itemRequest.getUnitPrice());
            item.setNotes(itemRequest.getNotes());

            BigDecimal itemTotal = itemRequest.getUnitPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            item.setTotalPrice(itemTotal);
            total = total.add(itemTotal);
            items.add(item);
        }

        invoice.setItems(items);
        invoice.setTotalAmount(total);
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setRemainingAmount(total);

        MismatchResult mismatch = validateMismatch(invoice);
        invoice.setHasMismatch(mismatch.hasMismatch);
        invoice.setMismatchWarning(mismatch.warningText);

        SupplierInvoice saved = supplierInvoiceRepository.save(invoice);
        log.info("Created supplier invoice {} for goods receipt {}", saved.getInvoiceCode(), goodsReceipt.getReceiptCode());

        return convertToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SupplierInvoiceResponse> getAllInvoices() {
        return supplierInvoiceRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SupplierInvoiceResponse getInvoiceById(Long invoiceId) {
        SupplierInvoice invoice = supplierInvoiceRepository.findByIdWithItems(invoiceId);
        if (invoice == null) {
            throw new RuntimeException("Supplier invoice not found with id: " + invoiceId);
        }
        return convertToResponse(invoice);
    }

    @Transactional
    public SupplierInvoiceResponse verifyInvoice(Long invoiceId, VerifyInvoiceRequest request, Long userId) {
        SupplierInvoice invoice = supplierInvoiceRepository.findByIdWithItems(invoiceId);
        if (invoice == null) {
            throw new RuntimeException("Supplier invoice not found");
        }
        if (invoice.getStatus() != InvoiceStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("Only PENDING_VERIFICATION invoice can be verified");
        }

        User verifiedBy = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        MismatchResult mismatch = validateMismatch(invoice);
        invoice.setHasMismatch(mismatch.hasMismatch);
        invoice.setMismatchWarning(mismatch.warningText);

        if (mismatch.hasMismatch) {
            supplierInvoiceRepository.save(invoice);
            throw new IllegalStateException(
                "Invoice has mismatch with goods receipt. Reject or correct invoice before verification");
        }

        invoice.setStatus(InvoiceStatus.VERIFIED);
        invoice.setVerifiedBy(verifiedBy);
        invoice.setVerifiedAt(LocalDateTime.now());
        invoice.setVerificationNotes(request != null ? request.getVerificationNotes() : null);

        SupplierInvoice saved = supplierInvoiceRepository.save(invoice);
        return convertToResponse(saved);
    }

    @Transactional
    public SupplierInvoiceResponse rejectInvoice(Long invoiceId, RejectInvoiceRequest request, Long userId) {
        SupplierInvoice invoice = supplierInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Supplier invoice not found"));

        if (invoice.getStatus() != InvoiceStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("Only PENDING_VERIFICATION invoice can be rejected");
        }

        User verifiedBy = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        invoice.setStatus(InvoiceStatus.REJECTED);
        invoice.setVerifiedBy(verifiedBy);
        invoice.setVerifiedAt(LocalDateTime.now());
        invoice.setRejectionReason(request != null ? request.getReason() : null);

        SupplierInvoice saved = supplierInvoiceRepository.save(invoice);
        return convertToResponse(saved);
    }

    @Transactional
    public SupplierInvoiceResponse payInvoice(Long invoiceId, PaymentInvoiceRequest request, Long userId) {
        if (request == null || request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0");
        }

        SupplierInvoice invoice = supplierInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Supplier invoice not found"));

        if (invoice.getStatus() != InvoiceStatus.VERIFIED && invoice.getStatus() != InvoiceStatus.PARTIALLY_PAID) {
            throw new IllegalStateException("Only VERIFIED or PARTIALLY_PAID invoices can be paid");
        }

        if (request.getAmount().compareTo(invoice.getRemainingAmount()) > 0) {
            throw new IllegalArgumentException("Payment exceeds remaining amount");
        }

        String txRef = normalizeTransactionReference(request.getTransactionReference());
        if (txRef != null
                && paymentRepository.existsBySupplierInvoice_InvoiceIdAndTransactionReference(invoiceId, txRef)) {
            throw new IllegalArgumentException("Transaction reference already exists for this invoice");
        }

        if (txRef == null) {
            txRef = generateTransactionReference();
        }

        User paidBy = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Payment payment = new Payment();
        payment.setSupplierInvoice(invoice);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setAmount(request.getAmount());
        payment.setMethod(request.getMethod());
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionReference(txRef);
        payment.setNotes(request.getNotes());
        paymentRepository.save(payment);

        BigDecimal newPaidAmount = invoice.getPaidAmount().add(request.getAmount());
        BigDecimal newRemaining = invoice.getTotalAmount().subtract(newPaidAmount);

        invoice.setPaidAmount(newPaidAmount);
        invoice.setRemainingAmount(newRemaining);
        invoice.setPaidBy(paidBy);
        invoice.setPaidAt(LocalDateTime.now());
        invoice.setStatus(newRemaining.compareTo(BigDecimal.ZERO) == 0
                ? InvoiceStatus.PAID
                : InvoiceStatus.PARTIALLY_PAID);

        SupplierInvoice saved = supplierInvoiceRepository.save(invoice);
        return convertToResponse(saved);
    }

    private MismatchResult validateMismatch(SupplierInvoice invoice) {
        if (invoice.getGoodsReceipt() == null || invoice.getGoodsReceipt().getPurchaseOrder() == null) {
            return new MismatchResult(false, null);
        }

        List<PurchaseOrderItem> poItems = invoice.getGoodsReceipt().getPurchaseOrder().getItems();
        if (poItems == null || poItems.isEmpty()) {
            return new MismatchResult(false, null);
        }

        Map<Long, PurchaseOrderItem> poItemByMedicine = new HashMap<>();
        for (PurchaseOrderItem poItem : poItems) {
            if (poItem.getMedicine() != null) {
                poItemByMedicine.put(poItem.getMedicine().getMedicineId(), poItem);
            }
        }

        List<String> mismatches = new ArrayList<>();
        for (SupplierInvoiceItem invoiceItem : invoice.getItems()) {
            Long medicineId = invoiceItem.getMedicine() != null ? invoiceItem.getMedicine().getMedicineId() : null;
            if (medicineId == null || !poItemByMedicine.containsKey(medicineId)) {
                mismatches.add("Medicine not found in purchase order: " + medicineId);
                continue;
            }

            PurchaseOrderItem poItem = poItemByMedicine.get(medicineId);
            Integer receivedQty = poItem.getReceivedQuantity() != null ? poItem.getReceivedQuantity() : poItem.getRequestedQuantity();
            if (receivedQty != null && !receivedQty.equals(invoiceItem.getQuantity())) {
                mismatches.add("Quantity mismatch for medicine " + poItem.getMedicine().getName()
                        + " (received=" + receivedQty + ", invoice=" + invoiceItem.getQuantity() + ")");
            }

            if (poItem.getUnitPrice() != null && invoiceItem.getUnitPrice() != null
                    && poItem.getUnitPrice().compareTo(invoiceItem.getUnitPrice()) != 0) {
                mismatches.add("Unit price mismatch for medicine " + poItem.getMedicine().getName()
                        + " (PO=" + poItem.getUnitPrice() + ", invoice=" + invoiceItem.getUnitPrice() + ")");
            }
        }

        for (PurchaseOrderItem poItem : poItems) {
            Long medicineId = poItem.getMedicine() != null ? poItem.getMedicine().getMedicineId() : null;
            boolean missingInInvoice = invoice.getItems().stream()
                    .noneMatch(item -> item.getMedicine() != null && item.getMedicine().getMedicineId().equals(medicineId));
            if (medicineId != null && missingInInvoice) {
                mismatches.add("Missing medicine in invoice: " + poItem.getMedicine().getName());
            }
        }

        if (mismatches.isEmpty()) {
            return new MismatchResult(false, null);
        }
        return new MismatchResult(true, String.join("; ", mismatches));
    }

    private SupplierInvoiceResponse convertToResponse(SupplierInvoice invoice) {
        List<Payment> payments = paymentRepository.findBySupplierInvoice_InvoiceId(invoice.getInvoiceId());

        return SupplierInvoiceResponse.builder()
                .invoiceId(invoice.getInvoiceId())
                .invoiceCode(invoice.getInvoiceCode())
                .supplier(convertSupplierToResponse(invoice.getSupplier()))
                .goodsReceipt(invoice.getGoodsReceipt() != null
                        ? SupplierInvoiceResponse.GoodsReceiptInfo.builder()
                                .receiptId(invoice.getGoodsReceipt().getReceiptId())
                                .receiptCode(invoice.getGoodsReceipt().getReceiptCode())
                                .purchaseOrderCode(invoice.getGoodsReceipt().getPurchaseOrder() != null
                                        ? invoice.getGoodsReceipt().getPurchaseOrder().getOrderCode()
                                        : null)
                                .build()
                        : null)
                .status(invoice.getStatus() != null ? invoice.getStatus().name() : null)
                .invoiceDate(invoice.getInvoiceDate())
                .dueDate(invoice.getDueDate())
                .totalAmount(invoice.getTotalAmount())
                .paidAmount(invoice.getPaidAmount())
                .remainingAmount(invoice.getRemainingAmount())
                .notes(invoice.getNotes())
                .verificationNotes(invoice.getVerificationNotes())
                .rejectionReason(invoice.getRejectionReason())
                .hasMismatch(invoice.getHasMismatch())
                .mismatchWarning(invoice.getMismatchWarning())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .verifiedAt(invoice.getVerifiedAt())
                .paidAt(invoice.getPaidAt())
                .createdBy(convertUserInfo(invoice.getCreatedBy()))
                .verifiedBy(convertUserInfo(invoice.getVerifiedBy()))
                .paidBy(convertUserInfo(invoice.getPaidBy()))
                .items(invoice.getItems() != null
                        ? invoice.getItems().stream().map(this::convertInvoiceItem).collect(Collectors.toList())
                        : List.of())
                .payments(payments.stream().map(this::convertPayment).collect(Collectors.toList()))
                .build();
    }

    private SupplierInvoiceResponse.SupplierInvoiceItemResponse convertInvoiceItem(SupplierInvoiceItem item) {
        return SupplierInvoiceResponse.SupplierInvoiceItemResponse.builder()
                .itemId(item.getInvoiceItemId())
                .medicine(item.getMedicine() != null
                        ? SupplierInvoiceResponse.MedicineInfo.builder()
                                .medicineId(item.getMedicine().getMedicineId())
                                .medicineName(item.getMedicine().getName())
                                .build()
                        : null)
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .notes(item.getNotes())
                .build();
    }

    private SupplierInvoiceResponse.PaymentInfo convertPayment(Payment payment) {
        return SupplierInvoiceResponse.PaymentInfo.builder()
                .paymentId(payment.getPaymentId())
                .paymentDate(payment.getPaymentDate())
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .transactionReference(payment.getTransactionReference())
                .status(payment.getStatus() != null ? payment.getStatus().name() : null)
                .notes(payment.getNotes())
                .build();
    }

    private String normalizeTransactionReference(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String generateTransactionReference() {
        return "PAY-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private SupplierInvoiceResponse.UserInfo convertUserInfo(User user) {
        if (user == null) {
            return null;
        }
        return SupplierInvoiceResponse.UserInfo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .build();
    }

    private SupplierResponse convertSupplierToResponse(Supplier supplier) {
        if (supplier == null) {
            return null;
        }
        return SupplierResponse.builder()
                .supplierId(supplier.getSupplierId())
                .supplierName(supplier.getSupplierName())
                .contactPerson(supplier.getContactPerson())
                .phoneNumber(supplier.getPhoneNumber())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .taxCode(supplier.getTaxCode())
                .qrBankTransferLink(supplier.getQrBankTransferLink())
                .status(supplier.getStatus() != null ? supplier.getStatus().name() : null)
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }

    private static class MismatchResult {
        private final boolean hasMismatch;
        private final String warningText;

        private MismatchResult(boolean hasMismatch, String warningText) {
            this.hasMismatch = hasMismatch;
            this.warningText = warningText;
        }
    }
}
