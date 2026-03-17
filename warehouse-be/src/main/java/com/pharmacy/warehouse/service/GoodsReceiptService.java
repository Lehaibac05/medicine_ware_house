package com.pharmacy.warehouse.service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pharmacy.warehouse.dto.ApproveGoodsReceiptRequest;
import com.pharmacy.warehouse.dto.CreateGoodsReceiptRequest;
import com.pharmacy.warehouse.dto.GoodsReceiptResponse;
import com.pharmacy.warehouse.dto.PurchaseOrderResponse;
import com.pharmacy.warehouse.model.Batch;
import com.pharmacy.warehouse.model.GoodsReceipt;
import com.pharmacy.warehouse.model.GoodsReceipt.ReceiptStatus;
import com.pharmacy.warehouse.model.PurchaseOrder;
import com.pharmacy.warehouse.model.PurchaseOrder.PurchaseOrderStatus;
import com.pharmacy.warehouse.model.PurchaseOrderItem;
import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.repository.BatchRepository;
import com.pharmacy.warehouse.repository.GoodsReceiptRepository;
import com.pharmacy.warehouse.repository.PurchaseOrderRepository;
import com.pharmacy.warehouse.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsReceiptService {

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final UserRepository userRepository;
    private final BatchRepository batchRepository;
    private final PurchaseOrderService purchaseOrderService;

    @Transactional(readOnly = true)
    public List<GoodsReceiptResponse> getAllGoodsReceipts() {
        return goodsReceiptRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GoodsReceiptResponse getGoodsReceiptById(Long id) {
        GoodsReceipt receipt = goodsReceiptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Goods receipt not found with id: " + id));
        return convertToResponse(receipt);
    }

    @Transactional(readOnly = true)
    public List<GoodsReceiptResponse> getPendingGoodsReceipts() {
        return goodsReceiptRepository.findPendingReceipts().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GoodsReceiptResponse getGoodsReceiptByPurchaseOrderId(Long purchaseOrderId) {
        GoodsReceipt receipt = goodsReceiptRepository.findByPurchaseOrder_PurchaseOrderId(purchaseOrderId)
                .orElseThrow(() -> new RuntimeException("Goods receipt not found for purchase order: " + purchaseOrderId));
        return convertToResponse(receipt);
    }

    // Bước 3: Nhân viên kho tiếp nhận thuốc
    @Transactional
    public GoodsReceiptResponse createGoodsReceipt(CreateGoodsReceiptRequest request, Long userId) {
        log.info("Creating goods receipt for purchase order: {}", request.getPurchaseOrderId());
        
        // Validate purchase order
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findByIdWithItems(request.getPurchaseOrderId());
        if (purchaseOrder == null) {
            throw new RuntimeException("Purchase order not found");
        }
        
        if (purchaseOrder.getStatus() != PurchaseOrderStatus.SHIPPING && 
            purchaseOrder.getStatus() != PurchaseOrderStatus.CONFIRMED) {
            throw new RuntimeException("Purchase order must be in SHIPPING or CONFIRMED status");
        }
        
        // Check if goods receipt already exists
        if (goodsReceiptRepository.findByPurchaseOrder_PurchaseOrderId(request.getPurchaseOrderId()).isPresent()) {
            throw new RuntimeException("Goods receipt already exists for this purchase order");
        }
        
        // Get user (WAREHOUSE_STAFF)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Create goods receipt
        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setPurchaseOrder(purchaseOrder);
        receipt.setReceivedBy(user);
        receipt.setReceivedAt(LocalDateTime.now());
        receipt.setQualityCheckNotes(request.getQualityCheckNotes());
        receipt.setQualityPassed(request.getQualityPassed());
        receipt.setStatus(ReceiptStatus.PENDING_APPROVAL);
        
        // Update purchase order items with received quantities
        for (CreateGoodsReceiptRequest.ReceivedItemInfo receivedItem : request.getReceivedItems()) {
            PurchaseOrderItem item = purchaseOrder.getItems().stream()
                    .filter(i -> i.getItemId().equals(receivedItem.getItemId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Purchase order item not found: " + receivedItem.getItemId()));
            
            item.setReceivedQuantity(receivedItem.getReceivedQuantity());
            item.setActualExpiryDate(receivedItem.getActualExpiryDate());
        }
        
        // Update purchase order status
        purchaseOrder.setStatus(PurchaseOrderStatus.RECEIVED);
        purchaseOrderRepository.save(purchaseOrder);
        
        GoodsReceipt savedReceipt = goodsReceiptRepository.save(receipt);
        log.info("Goods receipt created successfully with code: {}", savedReceipt.getReceiptCode());
        
        return convertToResponse(savedReceipt);
    }

    // Bước 4: Quản lý kho phê duyệt việc nhập kho
    @Transactional
    public GoodsReceiptResponse approveGoodsReceipt(Long receiptId, ApproveGoodsReceiptRequest request, Long userId) {
        log.info("Approving goods receipt: {}", receiptId);

        if (request == null || request.getApproved() == null) {
            throw new IllegalArgumentException("approved flag is required");
        }
        
        GoodsReceipt receipt = goodsReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new RuntimeException("Goods receipt not found"));
        
        if (receipt.getStatus() != ReceiptStatus.PENDING_APPROVAL) {
            throw new RuntimeException("Goods receipt must be in PENDING_APPROVAL status");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (request.getApproved()) {
            // Approve the receipt
            receipt.setStatus(ReceiptStatus.APPROVED);
            receipt.setApprovedBy(user);
            receipt.setApprovedAt(LocalDateTime.now());
            
            if (request.getNotes() != null && !request.getNotes().isEmpty()) {
                String currentNotes = receipt.getQualityCheckNotes() != null ? receipt.getQualityCheckNotes() : "";
                receipt.setQualityCheckNotes(currentNotes.isBlank()
                        ? request.getNotes()
                        : currentNotes + "\n" + request.getNotes());
            }
            
            // Update purchase order status
            PurchaseOrder purchaseOrder = receipt.getPurchaseOrder();
            purchaseOrder.setStatus(PurchaseOrderStatus.APPROVED);
            purchaseOrderRepository.save(purchaseOrder);
            
            // Create batches and update inventory
            createBatchesFromReceipt(receipt);
            
            log.info("Goods receipt approved successfully");
        } else {
            // Reject the receipt
            receipt.setStatus(ReceiptStatus.REJECTED);
            receipt.setApprovedBy(user);
            receipt.setApprovedAt(LocalDateTime.now());
            
            if (request.getNotes() != null) {
                receipt.setQualityCheckNotes(receipt.getQualityCheckNotes() + "\nReason: " + request.getNotes());
            }
            
            // Revert purchase order status
            PurchaseOrder purchaseOrder = receipt.getPurchaseOrder();
            purchaseOrder.setStatus(PurchaseOrderStatus.SHIPPING);
            purchaseOrderRepository.save(purchaseOrder);
            
            log.info("Goods receipt rejected");
        }
        
        GoodsReceipt updatedReceipt = goodsReceiptRepository.save(receipt);
        return convertToResponse(updatedReceipt);
    }

    // Tự động tạo Batch và cập nhật tồn kho sau khi phê duyệt
    private void createBatchesFromReceipt(GoodsReceipt receipt) {
        log.info("Creating batches from goods receipt: {}", receipt.getReceiptCode());
        
        PurchaseOrder purchaseOrder = receipt.getPurchaseOrder();
        
        for (PurchaseOrderItem item : purchaseOrder.getItems()) {
            if (item.getReceivedQuantity() != null && item.getReceivedQuantity() > 0) {
                Batch batch = new Batch();
                batch.setMedicine(item.getMedicine());
                batch.setWarehouse(purchaseOrder.getWarehouse());
                batch.setQuantity(item.getReceivedQuantity());
                LocalDate expiryDate = item.getActualExpiryDate() != null
                        ? item.getActualExpiryDate()
                        : item.getExpectedExpiryDate();
                if (expiryDate == null) {
                    expiryDate = LocalDate.now().plusYears(2);
                }
                batch.setExpiryDate(expiryDate);
                batch.setStatus("AVAILABLE");
                
                // Generate lot number from receipt code
                batch.setLotNumber(receipt.getReceiptCode() + "-" + item.getItemId() + "-" + (System.currentTimeMillis() % 1000));
                
                // Set manufacture date (estimate based on expiry date)
                batch.setManufactureDate(expiryDate.minusYears(2));
                
                batchRepository.save(batch);
                log.info("Created batch {} for medicine {} with quantity {}", 
                        batch.getLotNumber(), item.getMedicine().getName(), batch.getQuantity());
            }
        }
    }

    private GoodsReceiptResponse convertToResponse(GoodsReceipt receipt) {
        PurchaseOrderResponse purchaseOrderResponse = null;
        if (receipt.getPurchaseOrder() != null) {
            purchaseOrderResponse = purchaseOrderService.getPurchaseOrderById(receipt.getPurchaseOrder().getPurchaseOrderId());
        }
        
        return GoodsReceiptResponse.builder()
                .receiptId(receipt.getReceiptId())
                .receiptCode(receipt.getReceiptCode())
                .purchaseOrder(purchaseOrderResponse)
                .receivedBy(receipt.getReceivedBy() != null ? 
                        GoodsReceiptResponse.UserInfo.builder()
                                .userId(receipt.getReceivedBy().getUserId())
                                .username(receipt.getReceivedBy().getUsername())
                                .fullName(receipt.getReceivedBy().getFullName())
                                .build() : null)
                .approvedBy(receipt.getApprovedBy() != null ? 
                        GoodsReceiptResponse.UserInfo.builder()
                                .userId(receipt.getApprovedBy().getUserId())
                                .username(receipt.getApprovedBy().getUsername())
                                .fullName(receipt.getApprovedBy().getFullName())
                                .build() : null)
                .status(receipt.getStatus().name())
                .qualityCheckNotes(receipt.getQualityCheckNotes())
                .qualityPassed(receipt.getQualityPassed())
                .receivedAt(receipt.getReceivedAt())
                .approvedAt(receipt.getApprovedAt())
                .createdAt(receipt.getCreatedAt())
                .build();
    }
}
