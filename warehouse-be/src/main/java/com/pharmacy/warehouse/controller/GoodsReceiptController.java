package com.pharmacy.warehouse.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pharmacy.warehouse.dto.ApproveGoodsReceiptRequest;
import com.pharmacy.warehouse.dto.CreateGoodsReceiptRequest;
import com.pharmacy.warehouse.dto.GoodsReceiptResponse;
import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.repository.UserRepository;
import com.pharmacy.warehouse.service.GoodsReceiptService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/goods-receipts")
@RequiredArgsConstructor
public class GoodsReceiptController {

    private final GoodsReceiptService goodsReceiptService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<GoodsReceiptResponse>> getAllGoodsReceipts() {
        log.info("GET /goods-receipts - Fetching all goods receipts");
        List<GoodsReceiptResponse> receipts = goodsReceiptService.getAllGoodsReceipts();
        return ResponseEntity.ok(receipts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoodsReceiptResponse> getGoodsReceiptById(@PathVariable Long id) {
        log.info("GET /goods-receipts/{} - Fetching goods receipt", id);
        GoodsReceiptResponse receipt = goodsReceiptService.getGoodsReceiptById(id);
        return ResponseEntity.ok(receipt);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<GoodsReceiptResponse>> getPendingGoodsReceipts() {
        log.info("GET /goods-receipts/pending - Fetching pending goods receipts");
        List<GoodsReceiptResponse> receipts = goodsReceiptService.getPendingGoodsReceipts();
        return ResponseEntity.ok(receipts);
    }

    @GetMapping("/purchase-order/{purchaseOrderId}")
    public ResponseEntity<GoodsReceiptResponse> getGoodsReceiptByPurchaseOrderId(@PathVariable Long purchaseOrderId) {
        log.info("GET /goods-receipts/purchase-order/{} - Fetching goods receipt", purchaseOrderId);
        GoodsReceiptResponse receipt = goodsReceiptService.getGoodsReceiptByPurchaseOrderId(purchaseOrderId);
        return ResponseEntity.ok(receipt);
    }

    // Bước 3: Nhân viên kho tiếp nhận thuốc
    @PostMapping
    public ResponseEntity<GoodsReceiptResponse> createGoodsReceipt(
            @RequestBody CreateGoodsReceiptRequest request,
            Authentication authentication) {
        log.info("POST /goods-receipts - Creating goods receipt by user: {}", 
                authentication != null ? authentication.getName() : "unknown");

        User user = resolveAuthenticatedUser(authentication);
        
        GoodsReceiptResponse receipt = goodsReceiptService.createGoodsReceipt(request, user.getUserId());
        return ResponseEntity.ok(receipt);
    }

    // Bước 4: Quản lý kho phê duyệt việc nhập kho
    @PostMapping("/{id}/approve")
    public ResponseEntity<GoodsReceiptResponse> approveGoodsReceipt(
            @PathVariable Long id,
            @RequestBody ApproveGoodsReceiptRequest request,
            Authentication authentication) {
        log.info("POST /goods-receipts/{}/approve - Approving goods receipt by user: {}", 
                id, authentication != null ? authentication.getName() : "unknown");

        User user = resolveAuthenticatedUser(authentication);
        
        GoodsReceiptResponse receipt = goodsReceiptService.approveGoodsReceipt(id, request, user.getUserId());
        return ResponseEntity.ok(receipt);
    }

    private User resolveAuthenticatedUser(Authentication authentication) {
        String principal = authentication != null ? authentication.getName() : null;
        if (principal == null || principal.isBlank()) {
            throw new RuntimeException("Unauthorized");
        }

        return userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
