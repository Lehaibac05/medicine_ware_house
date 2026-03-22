package com.pharmacy.warehouse.controller;

import com.pharmacy.warehouse.dto.CreateOrderRequest;
import com.pharmacy.warehouse.dto.OrderItemDTO;
import com.pharmacy.warehouse.dto.OrderResponse;
import com.pharmacy.warehouse.dto.UpdateOrderStatusRequest;
import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.repository.UserRepository;
import com.pharmacy.warehouse.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    @GetMapping("/issue-requests")
    public ResponseEntity<List<OrderResponse>> getAllIssueRequests() {
        return ResponseEntity.ok(orderService.getAllIssueRequests());
    }

    @GetMapping("/issue-requests/my")
    public ResponseEntity<List<OrderResponse>> getMyIssueRequests(Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(orderService.getIssueRequestsByUser(user.getUserId()));
    }

    @GetMapping("/issue-requests/{id}")
    public ResponseEntity<OrderResponse> getIssueRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getIssueRequestById(id));
    }

    @PostMapping("/issue-requests")
    public ResponseEntity<OrderResponse> createIssueRequest(
            @RequestBody CreateOrderRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createIssueRequest(request, user.getUserId()));
    }

    @PostMapping("/issue-requests/{id}/approve")
    public ResponseEntity<OrderResponse> approveIssueRequest(
            @PathVariable Long id,
            @RequestBody(required = false) UpdateOrderStatusRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        Boolean allowPartial = request != null ? request.getAllowPartial() : Boolean.FALSE;
        return ResponseEntity.ok(orderService.approveIssueRequest(id, allowPartial, user.getUserId()));
    }

    @PostMapping("/issue-requests/{id}/reject")
    public ResponseEntity<OrderResponse> rejectIssueRequest(
            @PathVariable Long id,
            @RequestBody(required = false) UpdateOrderStatusRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(orderService.rejectIssueRequest(id, reason, user.getUserId()));
    }

    @PostMapping("/issues/execute/{requestId}")
    public ResponseEntity<OrderResponse> executeIssue(
            @PathVariable Long requestId,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(orderService.executeIssue(requestId, user.getUserId()));
    }

    @GetMapping("/issues/history")
    public ResponseEntity<List<OrderItemDTO>> getIssueHistory(
            @RequestParam(required = false) Long medicineId,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(orderService.getIssueHistory(medicineId, department, fromDate, toDate));
    }

    @GetMapping("/issues/stock-insight")
    public ResponseEntity<OrderResponse> getStockInsight(
            @RequestParam Long medicineId,
            @RequestParam Long warehouseId,
            @RequestParam(required = false) Integer quantity) {
        return ResponseEntity.ok(orderService.getStockInsight(medicineId, warehouseId, quantity));
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new RuntimeException("Unauthorized");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
