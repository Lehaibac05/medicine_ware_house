package com.pharmacy.warehouse.controller;

import java.time.LocalDateTime;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pharmacy.warehouse.dto.CreateMedicineRequestRequest;
import com.pharmacy.warehouse.dto.MedicineRequestResponse;
import com.pharmacy.warehouse.model.MedicineRequest.RequestStatus;
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
    public ResponseEntity<Page<MedicineRequestResponse>> getAllRequests(
            @RequestParam(required = false) String medicineName,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            Pageable pageable) {
        log.info("GET /medicine-requests - Fetching all medicine requests with filters and pagination");
        return ResponseEntity.ok(medicineRequestService.getAllRequests(medicineName, status, startDate, endDate, pageable));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<MedicineRequestResponse>> getMyRequests(
            @RequestParam(required = false) String medicineName,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            Pageable pageable,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        if (username == null || username.isBlank()) {
            throw new RuntimeException("Unauthorized");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("GET /medicine-requests/my - Fetching requests for user: {} with filters and pagination", username);
        return ResponseEntity.ok(medicineRequestService.getMyRequests(user.getUserId(), medicineName, status, startDate, endDate, pageable));
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
