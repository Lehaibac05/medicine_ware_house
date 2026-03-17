package com.pharmacy.warehouse.controller;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pharmacy.warehouse.dto.CreateMedicineRequestRequest;
import com.pharmacy.warehouse.dto.MedicineRequestResponse;
import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.repository.UserRepository;
import com.pharmacy.warehouse.service.MedicineRequestService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/medicine-requests")
@RequiredArgsConstructor
public class MedicineRequestController {

    private final MedicineRequestService medicineRequestService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<MedicineRequestResponse>> getAllRequests() {
        log.info("GET /medicine-requests - Fetching all medicine requests");
        return ResponseEntity.ok(medicineRequestService.getAllRequests());
    }

    @GetMapping("/my")
    public ResponseEntity<List<MedicineRequestResponse>> getMyRequests(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        if (username == null || username.isBlank()) {
            throw new RuntimeException("Unauthorized");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("GET /medicine-requests/my - Fetching requests for user: {}", username);
        return ResponseEntity.ok(medicineRequestService.getMyRequests(user.getUserId()));
    }

    @PostMapping
    public ResponseEntity<MedicineRequestResponse> createRequest(
            @Valid @RequestBody CreateMedicineRequestRequest request,
            Authentication authentication) {
        log.info("POST /medicine-requests - Creating request by user: {}",
                authentication != null ? authentication.getName() : "unknown");

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(medicineRequestService.createRequest(request, user.getUserId()));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<MedicineRequestResponse> approveRequest(@PathVariable Long id) {
        log.info("POST /medicine-requests/{}/approve - Approving request", id);
        return ResponseEntity.ok(medicineRequestService.approveRequest(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<MedicineRequestResponse> rejectRequest(@PathVariable Long id) {
        log.info("POST /medicine-requests/{}/reject - Rejecting request", id);
        return ResponseEntity.ok(medicineRequestService.rejectRequest(id));
    }
}
