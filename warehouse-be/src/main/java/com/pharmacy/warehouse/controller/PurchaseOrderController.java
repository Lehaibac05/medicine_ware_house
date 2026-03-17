package com.pharmacy.warehouse.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pharmacy.warehouse.dto.CreatePurchaseOrderRequest;
import com.pharmacy.warehouse.dto.PurchaseOrderResponse;
import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.repository.UserRepository;
import com.pharmacy.warehouse.service.PurchaseOrderEmailService;
import com.pharmacy.warehouse.service.PurchaseOrderPdfService;
import com.pharmacy.warehouse.service.PurchaseOrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseOrderPdfService purchaseOrderPdfService;
    private final PurchaseOrderEmailService purchaseOrderEmailService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<PurchaseOrderResponse>> getAllPurchaseOrders() {
        log.info("GET /purchase-orders - Fetching all purchase orders");
        List<PurchaseOrderResponse> orders = purchaseOrderService.getAllPurchaseOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponse> getPurchaseOrderById(@PathVariable Long id) {
        log.info("GET /purchase-orders/{} - Fetching purchase order", id);
        PurchaseOrderResponse order = purchaseOrderService.getPurchaseOrderById(id);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> exportPurchaseOrderPdf(@PathVariable Long id) {
        log.info("GET /purchase-orders/{}/pdf - Export purchase order PDF", id);

        PurchaseOrderResponse order = purchaseOrderService.getPurchaseOrderById(id);
        byte[] pdfBytes = purchaseOrderPdfService.generatePurchaseOrderPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
            ContentDisposition.attachment().filename(order.getOrderCode() + ".pdf").build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PurchaseOrderResponse>> getPurchaseOrdersByStatus(@PathVariable String status) {
        log.info("GET /purchase-orders/status/{} - Fetching purchase orders by status", status);
        List<PurchaseOrderResponse> orders = purchaseOrderService.getPurchaseOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<List<PurchaseOrderResponse>> getPurchaseOrdersByWarehouse(@PathVariable Long warehouseId) {
        log.info("GET /purchase-orders/warehouse/{} - Fetching purchase orders by warehouse", warehouseId);
        List<PurchaseOrderResponse> orders = purchaseOrderService.getPurchaseOrdersByWarehouse(warehouseId);
        return ResponseEntity.ok(orders);
    }

    // Bước 1: Quản lý kho tạo yêu cầu nhập hàng
    @PostMapping
    public ResponseEntity<PurchaseOrderResponse> createPurchaseOrder(
            @RequestBody CreatePurchaseOrderRequest request,
            Authentication authentication) {
        log.info("POST /purchase-orders - Creating purchase order by user: {}", 
                authentication != null ? authentication.getName() : "unknown");
        
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        PurchaseOrderResponse order = purchaseOrderService.createPurchaseOrder(request, user.getUserId());
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponse> updatePurchaseOrder(
            @PathVariable Long id,
            @RequestBody CreatePurchaseOrderRequest request) {
        log.info("PUT /purchase-orders/{} - Updating purchase order", id);
        PurchaseOrderResponse order = purchaseOrderService.updatePurchaseOrder(id, request);
        return ResponseEntity.ok(order);
    }

    // Bước 2: Xác nhận đơn hàng
    @PostMapping("/{id}/confirm")
    public ResponseEntity<PurchaseOrderResponse> confirmPurchaseOrder(@PathVariable Long id) {
        log.info("POST /purchase-orders/{}/confirm - Confirming purchase order", id);
        PurchaseOrderResponse order = purchaseOrderService.confirmPurchaseOrder(id);
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<PurchaseOrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        log.info("PUT /purchase-orders/{}/status - Updating order status", id);
        String status = request.get("status");
        PurchaseOrderResponse order = purchaseOrderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, String>> cancelPurchaseOrder(
            @PathVariable Long id,
            Authentication authentication) {
        log.info("POST /purchase-orders/{}/cancel - Cancelling purchase order by user: {}", 
                id, authentication != null ? authentication.getName() : "unknown");
        
        purchaseOrderService.cancelPurchaseOrder(id);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Purchase order cancelled successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/send-email")
    public ResponseEntity<Map<String, String>> sendPurchaseOrderEmail(@PathVariable Long id) {
        log.info("POST /purchase-orders/{}/send-email - Sending purchase order email", id);

        PurchaseOrderResponse order = purchaseOrderService.getPurchaseOrderById(id);
        purchaseOrderEmailService.sendPurchaseOrderEmail(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Purchase order email sent successfully");
        response.put("orderCode", order.getOrderCode());
        response.put("supplierEmail", order.getSupplier() != null ? order.getSupplier().getEmail() : "");
        return ResponseEntity.ok(response);
    }
}
