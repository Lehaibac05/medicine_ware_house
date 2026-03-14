package com.pharmacy.warehouse.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final MedicineRepository medicineRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getPurchaseOrderById(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findByIdWithItems(id);
        if (order == null) {
            throw new RuntimeException("Purchase order not found with id: " + id);
        }
        return convertToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> getPurchaseOrdersByStatus(String status) {
        PurchaseOrderStatus orderStatus = PurchaseOrderStatus.valueOf(status);
        return purchaseOrderRepository.findByStatus(orderStatus).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> getPurchaseOrdersByWarehouse(Long warehouseId) {
        return purchaseOrderRepository.findByWarehouse_WarehouseId(warehouseId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // Bước 1: Quản lý kho tạo yêu cầu nhập hàng
    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(CreatePurchaseOrderRequest request, Long userId) {
        log.info("Creating purchase order for supplier: {}", request.getSupplierId());
        
        // Validate supplier
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
        
        // Validate warehouse
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));
        
        // Get user (WAREHOUSE_MANAGER)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Create purchase order
        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setSupplier(supplier);
        purchaseOrder.setWarehouse(warehouse);
        purchaseOrder.setCreatedBy(user);
        purchaseOrder.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        purchaseOrder.setNotes(request.getNotes());
        purchaseOrder.setStatus(PurchaseOrderStatus.PENDING);
        
        // Calculate total amount and create items
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (CreatePurchaseOrderRequest.PurchaseOrderItemRequest itemRequest : request.getItems()) {
            Medicine medicine = medicineRepository.findById(itemRequest.getMedicineId())
                    .orElseThrow(() -> new RuntimeException("Medicine not found with id: " + itemRequest.getMedicineId()));
            
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPurchaseOrder(purchaseOrder);
            item.setMedicine(medicine);
            item.setRequestedQuantity(itemRequest.getRequestedQuantity());
            item.setUnitPrice(itemRequest.getUnitPrice());
            
            BigDecimal itemTotal = itemRequest.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getRequestedQuantity()));
            item.setTotalPrice(itemTotal);
            totalAmount = totalAmount.add(itemTotal);
            
            item.setExpectedExpiryDate(itemRequest.getExpectedExpiryDate());
            item.setNotes(itemRequest.getNotes());
            
            if (purchaseOrder.getItems() == null) {
                purchaseOrder.setItems(new java.util.ArrayList<>());
            }
            purchaseOrder.getItems().add(item);
        }
        
        purchaseOrder.setTotalAmount(totalAmount);
        
        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);
        log.info("Purchase order created successfully with code: {}", savedOrder.getOrderCode());
        
        return convertToResponse(savedOrder);
    }

    // Bước 2: Nhà cung cấp xác nhận và chuẩn bị
    @Transactional
    public PurchaseOrderResponse confirmPurchaseOrder(Long orderId) {
        log.info("Confirming purchase order: {}", orderId);
        
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));
        
        if (order.getStatus() != PurchaseOrderStatus.PENDING) {
            throw new RuntimeException("Order must be in PENDING status to confirm");
        }
        
        order.setStatus(PurchaseOrderStatus.CONFIRMED);
        PurchaseOrder updatedOrder = purchaseOrderRepository.save(order);
        
        log.info("Purchase order confirmed successfully");
        return convertToResponse(updatedOrder);
    }

    @Transactional
    public PurchaseOrderResponse updateOrderStatus(Long orderId, String status) {
        log.info("Updating purchase order {} status to: {}", orderId, status);
        
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));
        
        PurchaseOrderStatus newStatus = PurchaseOrderStatus.valueOf(status);
        order.setStatus(newStatus);
        
        PurchaseOrder updatedOrder = purchaseOrderRepository.save(order);
        log.info("Purchase order status updated successfully");
        
        return convertToResponse(updatedOrder);
    }

    @Transactional
    public void cancelPurchaseOrder(Long orderId) {
        log.info("Cancelling purchase order: {}", orderId);
        
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));
        
        if (order.getStatus() == PurchaseOrderStatus.RECEIVED || 
            order.getStatus() == PurchaseOrderStatus.APPROVED) {
            throw new RuntimeException("Cannot cancel order that has been received or approved");
        }
        
        order.setStatus(PurchaseOrderStatus.CANCELLED);
        purchaseOrderRepository.save(order);
        
        log.info("Purchase order cancelled successfully");
    }

    private PurchaseOrderResponse convertToResponse(PurchaseOrder order) {
        return PurchaseOrderResponse.builder()
                .purchaseOrderId(order.getPurchaseOrderId())
                .orderCode(order.getOrderCode())
                .supplier(convertSupplierToResponse(order.getSupplier()))
                .warehouse(PurchaseOrderResponse.WarehouseInfo.builder()
                        .warehouseId(order.getWarehouse().getWarehouseId())
                        .warehouseName(order.getWarehouse().getName())
                        .build())
                .createdBy(PurchaseOrderResponse.UserInfo.builder()
                        .userId(order.getCreatedBy().getUserId())
                        .username(order.getCreatedBy().getUsername())
                        .fullName(order.getCreatedBy().getFullName())
                        .build())
                .status(order.getStatus().name())
                .expectedDeliveryDate(order.getExpectedDeliveryDate())
                .totalAmount(order.getTotalAmount())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(order.getItems() != null ? order.getItems().stream()
                        .map(this::convertItemToResponse)
                        .collect(Collectors.toList()) : null)
                .build();
    }

    private PurchaseOrderResponse.PurchaseOrderItemResponse convertItemToResponse(PurchaseOrderItem item) {
        return PurchaseOrderResponse.PurchaseOrderItemResponse.builder()
                .itemId(item.getItemId())
                .medicine(PurchaseOrderResponse.MedicineInfo.builder()
                        .medicineId(item.getMedicine().getMedicineId())
                        .medicineName(item.getMedicine().getName())
                        .sku(item.getMedicine().getManufacturer())
                        .build())
                .requestedQuantity(item.getRequestedQuantity())
                .receivedQuantity(item.getReceivedQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .expectedExpiryDate(item.getExpectedExpiryDate())
                .actualExpiryDate(item.getActualExpiryDate())
                .notes(item.getNotes())
                .build();
    }

    private SupplierResponse convertSupplierToResponse(Supplier supplier) {
        return SupplierResponse.builder()
                .supplierId(supplier.getSupplierId())
                .supplierName(supplier.getSupplierName())
                .contactPerson(supplier.getContactPerson())
                .phoneNumber(supplier.getPhoneNumber())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .taxCode(supplier.getTaxCode())
                .status(supplier.getStatus().name())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }
}
