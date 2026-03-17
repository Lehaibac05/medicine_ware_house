package com.pharmacy.warehouse.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pharmacy.warehouse.dto.CreateMedicineRequestRequest;
import com.pharmacy.warehouse.dto.MedicineRequestResponse;
import com.pharmacy.warehouse.model.Medicine;
import com.pharmacy.warehouse.model.MedicineRequest;
import com.pharmacy.warehouse.model.MedicineRequestItem;
import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.model.Warehouse;
import com.pharmacy.warehouse.repository.MedicineRepository;
import com.pharmacy.warehouse.repository.MedicineRequestRepository;
import com.pharmacy.warehouse.repository.UserRepository;
import com.pharmacy.warehouse.repository.WarehouseRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicineRequestService {

    private final MedicineRequestRepository medicineRequestRepository;
    private final MedicineRepository medicineRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<MedicineRequestResponse> getAllRequests() {
        return medicineRequestRepository.findAllWithItems().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MedicineRequestResponse> getMyRequests(Long userId) {
        return medicineRequestRepository.findAllByRequestedByUserIdWithItems(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MedicineRequestResponse createRequest(CreateMedicineRequestRequest request, Long userId) {
        log.info("Creating medicine request by user: {}", userId);

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Request must include at least one item");
        }

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        MedicineRequest medicineRequest = new MedicineRequest();
        medicineRequest.setWarehouse(warehouse);
        medicineRequest.setRequestedBy(user);
        medicineRequest.setRequiredDate(request.getRequiredDate());
        medicineRequest.setNotes(request.getNotes());
        medicineRequest.setStatus(MedicineRequest.RequestStatus.PENDING);

        List<MedicineRequestItem> items = new ArrayList<>();
        for (CreateMedicineRequestRequest.ItemRequest itemRequest : request.getItems()) {
            if (itemRequest.getQuantity() == null || itemRequest.getQuantity() <= 0) {
                throw new IllegalArgumentException("Requested quantity must be greater than zero");
            }

            Medicine medicine = medicineRepository.findById(itemRequest.getMedicineId())
                    .orElseThrow(() -> new RuntimeException("Medicine not found with id: " + itemRequest.getMedicineId()));

            MedicineRequestItem item = new MedicineRequestItem();
            item.setRequest(medicineRequest);
            item.setMedicine(medicine);
            item.setQuantity(itemRequest.getQuantity());
            item.setNotes(itemRequest.getNotes());
            items.add(item);
        }

        medicineRequest.setItems(items);
        MedicineRequest saved = medicineRequestRepository.save(medicineRequest);
        log.info("Medicine request created: {}", saved.getRequestId());
        return toResponse(saved);
    }

    @Transactional
    public MedicineRequestResponse approveRequest(Long requestId) {
        MedicineRequest request = getRequestEntity(requestId);
        if (request.getStatus() == null) {
            request.setStatus(MedicineRequest.RequestStatus.PENDING);
        }

        if (request.getStatus() != MedicineRequest.RequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be approved");
        }

        request.setStatus(MedicineRequest.RequestStatus.APPROVED);
        MedicineRequest saved = medicineRequestRepository.save(request);
        log.info("Medicine request approved: {}", requestId);
        return toResponse(saved);
    }

    @Transactional
    public MedicineRequestResponse rejectRequest(Long requestId) {
        MedicineRequest request = getRequestEntity(requestId);
        if (request.getStatus() == null) {
            request.setStatus(MedicineRequest.RequestStatus.PENDING);
        }

        if (request.getStatus() != MedicineRequest.RequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be rejected");
        }

        request.setStatus(MedicineRequest.RequestStatus.REJECTED);
        MedicineRequest saved = medicineRequestRepository.save(request);
        log.info("Medicine request rejected: {}", requestId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public MedicineRequest getRequestEntity(Long requestId) {
        MedicineRequest request = medicineRequestRepository.findByIdWithItems(requestId);
        if (request == null) {
            throw new RuntimeException("Medicine request not found with id: " + requestId);
        }
        return request;
    }

    private MedicineRequestResponse toResponse(MedicineRequest request) {
        MedicineRequest.RequestStatus effectiveStatus =
            request.getStatus() != null ? request.getStatus() : MedicineRequest.RequestStatus.PENDING;

        return MedicineRequestResponse.builder()
                .requestId(request.getRequestId())
                .warehouseId(request.getWarehouse() != null ? request.getWarehouse().getWarehouseId() : null)
                .warehouseName(request.getWarehouse() != null ? request.getWarehouse().getName() : null)
                .requiredDate(request.getRequiredDate())
                .notes(request.getNotes())
                .requestedBy(resolveRequestedBy(request.getRequestedBy()))
                .createdDate(request.getCreatedDate())
            .status(effectiveStatus.name())
                .items(request.getItems() != null
                        ? request.getItems().stream().map(this::toItemResponse).collect(Collectors.toList())
                        : List.of())
                .build();
    }

    private MedicineRequestResponse.ItemResponse toItemResponse(MedicineRequestItem item) {
        return MedicineRequestResponse.ItemResponse.builder()
                .medicineId(item.getMedicine() != null ? item.getMedicine().getMedicineId() : null)
                .medicineName(item.getMedicine() != null ? item.getMedicine().getName() : null)
                .quantity(item.getQuantity())
                .notes(item.getNotes())
                .build();
    }

    private String resolveRequestedBy(User user) {
        if (user == null) {
            return "Unknown";
        }
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        return user.getUsername();
    }
}
