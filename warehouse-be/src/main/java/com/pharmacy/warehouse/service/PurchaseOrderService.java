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
import com.pharmacy.warehouse.repository.PurchaseOrderItemRepository;
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
        private final PurchaseOrderItemRepository purchaseOrderItemRepository;
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
                PurchaseOrder order = getPurchaseOrderEntityById(id);
        return convertToResponse(order);
    }

        @Transactional(readOnly = true)
        public PurchaseOrder getPurchaseOrderEntityById(Long id) {
                PurchaseOrder order = purchaseOrderRepository.findByIdWithItems(id);
                if (order == null) {
                        throw new RuntimeException("Purchase order not found with id: " + id);
                }
                return order;
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

                rebuildItemsAndTotal(purchaseOrder, request);
        
        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);
        log.info("Purchase order created successfully with code: {}", savedOrder.getOrderCode());
        
        return convertToResponse(savedOrder);
    }

        @Transactional
        public PurchaseOrderResponse updatePurchaseOrder(Long orderId, CreatePurchaseOrderRequest request) {
                log.info("Updating purchase order: {}", orderId);

                PurchaseOrder order = getPurchaseOrderEntityById(orderId);
                if (order.getStatus() != PurchaseOrderStatus.PENDING) {
                        throw new IllegalStateException("Only PENDING purchase orders can be updated");
                }

                Supplier supplier = supplierRepository.findById(request.getSupplierId())
                                .orElseThrow(() -> new RuntimeException("Supplier not found"));

                Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

                if (order.getItems() != null && !order.getItems().isEmpty()) {
                        purchaseOrderItemRepository.deleteAll(order.getItems());
                        order.getItems().clear();
                }

                order.setSupplier(supplier);
                order.setWarehouse(warehouse);
                order.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
                order.setNotes(request.getNotes());

                rebuildItemsAndTotal(order, request);

                PurchaseOrder updatedOrder = purchaseOrderRepository.save(order);
                log.info("Purchase order updated successfully: {}", updatedOrder.getOrderCode());
                return convertToResponse(updatedOrder);
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

                if (newStatus == PurchaseOrderStatus.RECEIVED || newStatus == PurchaseOrderStatus.APPROVED) {
                        throw new IllegalStateException(
                                        "Use goods receipt workflow to move purchase order to RECEIVED/APPROVED");
                }

                if (newStatus == PurchaseOrderStatus.CONFIRMED && order.getStatus() == PurchaseOrderStatus.PENDING) {
                        throw new IllegalStateException("Use confirm endpoint to confirm purchase order");
                }

                if (order.getStatus() == newStatus) {
                        return convertToResponse(order);
                }

                if (!(order.getStatus() == PurchaseOrderStatus.CONFIRMED && newStatus == PurchaseOrderStatus.SHIPPING)) {
                        throw new IllegalStateException(
                                        String.format("Invalid status transition: %s -> %s", order.getStatus(), newStatus));
                }

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

        private void rebuildItemsAndTotal(PurchaseOrder order, CreatePurchaseOrderRequest request) {
                if (request.getItems() == null || request.getItems().isEmpty()) {
                        throw new IllegalArgumentException("Purchase order must include at least one item");
                }

                BigDecimal totalAmount = BigDecimal.ZERO;
                order.setItems(new java.util.ArrayList<>());

                for (CreatePurchaseOrderRequest.PurchaseOrderItemRequest itemRequest : request.getItems()) {
                        Medicine medicine = medicineRepository.findById(itemRequest.getMedicineId())
                                        .orElseThrow(() -> new RuntimeException("Medicine not found with id: " + itemRequest.getMedicineId()));

                        PurchaseOrderItem item = new PurchaseOrderItem();
                        item.setPurchaseOrder(order);
                        item.setMedicine(medicine);
                        item.setRequestedQuantity(itemRequest.getRequestedQuantity());
                        item.setUnitPrice(itemRequest.getUnitPrice());

                        BigDecimal itemTotal = itemRequest.getUnitPrice()
                                        .multiply(BigDecimal.valueOf(itemRequest.getRequestedQuantity()));
                        item.setTotalPrice(itemTotal);
                        totalAmount = totalAmount.add(itemTotal);

                        item.setExpectedExpiryDate(itemRequest.getExpectedExpiryDate());
                        item.setNotes(itemRequest.getNotes());
                        order.getItems().add(item);
                }

                order.setTotalAmount(totalAmount);
        }
}
