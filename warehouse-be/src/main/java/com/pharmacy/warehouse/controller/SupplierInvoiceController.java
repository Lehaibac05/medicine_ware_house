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

import com.pharmacy.warehouse.dto.CreateSupplierInvoiceRequest;
import com.pharmacy.warehouse.dto.PaymentInvoiceRequest;
import com.pharmacy.warehouse.dto.RejectInvoiceRequest;
import com.pharmacy.warehouse.dto.SupplierInvoiceResponse;
import com.pharmacy.warehouse.dto.VerifyInvoiceRequest;
import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.repository.UserRepository;
import com.pharmacy.warehouse.service.SupplierInvoiceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/supplier-invoices")
@RequiredArgsConstructor
public class SupplierInvoiceController {

    private final SupplierInvoiceService supplierInvoiceService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<SupplierInvoiceResponse> createInvoice(
            @RequestBody CreateSupplierInvoiceRequest request,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        SupplierInvoiceResponse response = supplierInvoiceService.createInvoice(request, user.getUserId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<SupplierInvoiceResponse>> getAllInvoices() {
        List<SupplierInvoiceResponse> invoices = supplierInvoiceService.getAllInvoices();
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierInvoiceResponse> getInvoiceById(@PathVariable Long id) {
        SupplierInvoiceResponse invoice = supplierInvoiceService.getInvoiceById(id);
        return ResponseEntity.ok(invoice);
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<SupplierInvoiceResponse> verifyInvoice(
            @PathVariable Long id,
            @RequestBody(required = false) VerifyInvoiceRequest request,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        SupplierInvoiceResponse response = supplierInvoiceService.verifyInvoice(id, request, user.getUserId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<SupplierInvoiceResponse> rejectInvoice(
            @PathVariable Long id,
            @RequestBody(required = false) RejectInvoiceRequest request,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        SupplierInvoiceResponse response = supplierInvoiceService.rejectInvoice(id, request, user.getUserId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<SupplierInvoiceResponse> payInvoice(
            @PathVariable Long id,
            @RequestBody PaymentInvoiceRequest request,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        SupplierInvoiceResponse response = supplierInvoiceService.payInvoice(id, request, user.getUserId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/pay/confirm")
    public ResponseEntity<Map<String, String>> payAndConfirm(
            @PathVariable Long id,
            @RequestBody PaymentInvoiceRequest request,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        SupplierInvoiceResponse response = supplierInvoiceService.payInvoice(id, request, user.getUserId());

        Map<String, String> result = new HashMap<>();
        result.put("message", "Payment processed successfully");
        result.put("invoiceCode", response.getInvoiceCode());
        result.put("status", response.getStatus());
        return ResponseEntity.ok(result);
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Authentication required");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
