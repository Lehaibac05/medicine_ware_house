package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.dto.CreateOrderRequest;
import com.pharmacy.warehouse.dto.IssueReportResponse;
import com.pharmacy.warehouse.dto.OrderItemDTO;
import com.pharmacy.warehouse.dto.OrderResponse;
import com.pharmacy.warehouse.model.Batch;
import com.pharmacy.warehouse.model.Medicine;
import com.pharmacy.warehouse.model.Order;
import com.pharmacy.warehouse.model.Order.IssueRequestStatus;
import com.pharmacy.warehouse.model.OrderItem;
import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.model.Warehouse;
import com.pharmacy.warehouse.repository.BatchRepository;
import com.pharmacy.warehouse.repository.MedicineRepository;
import com.pharmacy.warehouse.repository.OrderItemRepository;
import com.pharmacy.warehouse.repository.OrderRepository;
import com.pharmacy.warehouse.repository.UserRepository;
import com.pharmacy.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MedicineRepository medicineRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;

    @Transactional
    public OrderResponse createIssueRequest(CreateOrderRequest request, Long createdByUserId) {
        validateCreateRequest(request);

        User createdBy = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Medicine medicine = medicineRepository.findById(request.getMedicineId())
                .orElseThrow(() -> new RuntimeException("Medicine not found"));
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        Order issueRequest = new Order();
        issueRequest.setMedicine(medicine);
        issueRequest.setRequestedQuantity(request.getQuantity());
        issueRequest.setApprovedQuantity(null);
        issueRequest.setDepartment(request.getDepartment());
        issueRequest.setPurpose(request.getPurpose());
        issueRequest.setNeededDate(request.getNeededDate());
        issueRequest.setRequestDate(LocalDateTime.now());
        issueRequest.setStatus(IssueRequestStatus.PENDING);
        issueRequest.setCreatedBy(createdBy);
        issueRequest.setWarehouse(warehouse);
        issueRequest.setItems(new ArrayList<>());

        Order saved = orderRepository.save(issueRequest);
        return toOrderResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllIssueRequests() {
        return orderRepository.findAllIssueRequests().stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getIssueRequestsByUser(Long userId) {
        return orderRepository.findIssueRequestsByUser(userId).stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getIssueRequestById(Long id) {
        Order issueRequest = getIssueRequestEntity(id);
        return toOrderResponse(issueRequest);
    }

    @Transactional
    public OrderResponse approveIssueRequest(Long requestId, Boolean allowPartial, Long approvedByUserId) {
        Order issueRequest = getIssueRequestEntity(requestId);
        if (issueRequest.getStatus() != IssueRequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be approved");
        }

        User approvedBy = userRepository.findById(approvedByUserId)
                .orElseThrow(() -> new RuntimeException("Approver not found"));

        int totalStock = getAvailableStock(issueRequest.getMedicine().getMedicineId(), issueRequest.getWarehouse().getWarehouseId());
        int requested = issueRequest.getRequestedQuantity() == null ? 0 : issueRequest.getRequestedQuantity();
        boolean partialAllowed = Boolean.TRUE.equals(allowPartial);

        if (totalStock <= 0) {
            throw new IllegalStateException("No stock available for this medicine in selected warehouse");
        }

        if (totalStock < requested && !partialAllowed) {
            throw new IllegalStateException("Insufficient stock. Enable partial approval or reject request");
        }

        issueRequest.setApprovedQuantity(Math.min(requested, totalStock));
        issueRequest.setStatus(IssueRequestStatus.APPROVED);
        issueRequest.setApprovedBy(approvedBy);
        issueRequest.setApprovedAt(LocalDateTime.now());
        issueRequest.setRejectionReason(null);

        Order saved = orderRepository.save(issueRequest);
        return toOrderResponse(saved);
    }

    @Transactional
    public OrderResponse rejectIssueRequest(Long requestId, String reason, Long approvedByUserId) {
        Order issueRequest = getIssueRequestEntity(requestId);
        if (issueRequest.getStatus() != IssueRequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be rejected");
        }

        User approvedBy = userRepository.findById(approvedByUserId)
                .orElseThrow(() -> new RuntimeException("Approver not found"));

        issueRequest.setStatus(IssueRequestStatus.REJECTED);
        issueRequest.setApprovedBy(approvedBy);
        issueRequest.setApprovedAt(LocalDateTime.now());
        issueRequest.setRejectionReason(reason);

        return toOrderResponse(orderRepository.save(issueRequest));
    }

    @Transactional
    public OrderResponse executeIssue(Long requestId, Long issuedByUserId) {
        Order issueRequest = getIssueRequestEntity(requestId);
        if (issueRequest.getStatus() != IssueRequestStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED requests can be executed");
        }

        if (issueRequest.getApprovedQuantity() == null || issueRequest.getApprovedQuantity() <= 0) {
            throw new IllegalStateException("Approved quantity must be greater than zero");
        }

        List<OrderItem> existingIssueItems = orderItemRepository.findByOrderOrderId(issueRequest.getOrderId());
        if (!existingIssueItems.isEmpty()) {
            throw new IllegalStateException("Issue already executed for this request");
        }

        User issuedBy = userRepository.findById(issuedByUserId)
                .orElseThrow(() -> new RuntimeException("Issued user not found"));

        List<Batch> batches = batchRepository.findAvailableByMedicineAndWarehouseOrderByExpiry(
                issueRequest.getMedicine().getMedicineId(),
                issueRequest.getWarehouse().getWarehouseId());

        int remaining = issueRequest.getApprovedQuantity();
        for (Batch batch : batches) {
            if (remaining == 0) {
                break;
            }
            int available = batch.getQuantity() == null ? 0 : batch.getQuantity();
            if (available <= 0) {
                continue;
            }

            int take = Math.min(available, remaining);
            batch.setQuantity(available - take);
            batchRepository.save(batch);

            OrderItem issue = new OrderItem();
            issue.setOrder(issueRequest);
            issue.setMedicine(issueRequest.getMedicine());
            issue.setBatch(batch);
            issue.setWarehouse(issueRequest.getWarehouse());
            issue.setQuantity(take);
            issue.setIssuedBy(issuedBy);
            issue.setIssuedAt(LocalDateTime.now());
            orderItemRepository.save(issue);

            remaining -= take;
        }

        if (remaining > 0) {
            throw new IllegalStateException("Not enough stock to execute approved quantity");
        }

        issueRequest.setStatus(IssueRequestStatus.COMPLETED);
        issueRequest.setCompletedAt(LocalDateTime.now());
        Order saved = orderRepository.save(issueRequest);
        return toOrderResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderItemDTO> getIssueHistory(Long medicineId, String department, LocalDate fromDate, LocalDate toDate) {
        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = toDate != null ? toDate.atTime(23, 59, 59) : null;

        return orderItemRepository.findIssueHistory(
                        medicineId,
                        department != null && !department.isBlank() ? department : null,
                null,
                null,
                        fromDateTime,
                        toDateTime)
                .stream()
                .map(this::toIssueHistoryDto)
                .collect(Collectors.toList());
    }

        @Transactional(readOnly = true)
        public IssueReportResponse getIssueReport(
            Long medicineId,
            String department,
            Long warehouseId,
            Long issuedById,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size) {
        validatePageAndSize(page, size);
        validateDateRange(fromDate, toDate);

        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = toDate != null ? toDate.atTime(23, 59, 59) : null;

        List<IssueReportResponse.IssueReportItem> allItems = orderItemRepository.findIssueHistory(
                medicineId,
                department != null && !department.isBlank() ? department : null,
                warehouseId,
                issuedById,
                fromDateTime,
                toDateTime)
            .stream()
            .map(this::toIssueReportItem)
            .collect(Collectors.toList());

        int fromIndex = Math.min(page * size, allItems.size());
        int toIndex = Math.min(fromIndex + size, allItems.size());
        List<IssueReportResponse.IssueReportItem> content = allItems.subList(fromIndex, toIndex);

        return IssueReportResponse.builder()
            .items(new ArrayList<>(content))
            .page(page)
            .size(size)
            .totalElements(allItems.size())
            .totalPages(allItems.isEmpty() ? 0 : (int) Math.ceil((double) allItems.size() / size))
            .generatedAt(LocalDateTime.now())
            .exportColumns(List.of(
                "orderItemId",
                "requestId",
                "medicineId",
                "medicineName",
                "batchId",
                "lotNumber",
                "expiryDate",
                "quantity",
                "warehouseId",
                "warehouseName",
                "issuedById",
                "issuedByName",
                "department",
                "issuedAt"))
            .build();
        }

        @Transactional(readOnly = true)
        public IssueReportResponse exportIssueReport(
            Long medicineId,
            String department,
            Long warehouseId,
            Long issuedById,
            LocalDate fromDate,
            LocalDate toDate) {
        validateDateRange(fromDate, toDate);

        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = toDate != null ? toDate.atTime(23, 59, 59) : null;

        List<IssueReportResponse.IssueReportItem> items = orderItemRepository.findIssueHistory(
                medicineId,
                department != null && !department.isBlank() ? department : null,
                warehouseId,
                issuedById,
                fromDateTime,
                toDateTime)
            .stream()
            .map(this::toIssueReportItem)
            .collect(Collectors.toList());

        return IssueReportResponse.builder()
            .items(items)
            .page(0)
            .size(items.size())
            .totalElements(items.size())
            .totalPages(items.isEmpty() ? 0 : 1)
            .generatedAt(LocalDateTime.now())
            .exportColumns(List.of(
                "orderItemId",
                "requestId",
                "medicineId",
                "medicineName",
                "batchId",
                "lotNumber",
                "expiryDate",
                "quantity",
                "warehouseId",
                "warehouseName",
                "issuedById",
                "issuedByName",
                "department",
                "issuedAt"))
            .build();
        }

    @Transactional(readOnly = true)
    public OrderResponse getStockInsight(Long medicineId, Long warehouseId, Integer quantity) {
            Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new RuntimeException("Medicine not found"));
            Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

            OrderResponse response = new OrderResponse();
            response.setMedicineId(medicine.getMedicineId());
            response.setMedicineName(medicine.getName());
            response.setWarehouseId(warehouse.getWarehouseId());
            response.setWarehouseName(warehouse.getName());
            response.setRequestedQuantity(quantity != null ? quantity : 0);
            enrichStockInsights(response, medicine.getMedicineId(), warehouse.getWarehouseId(), response.getRequestedQuantity());
            return response;
            }

    private void validateCreateRequest(CreateOrderRequest request) {
        if (request.getMedicineId() == null) {
            throw new IllegalArgumentException("medicineId is required");
        }
        if (request.getWarehouseId() == null) {
            throw new IllegalArgumentException("warehouseId is required");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
    }

    private int getAvailableStock(Long medicineId, Long warehouseId) {
        return batchRepository.findAvailableByMedicineAndWarehouseOrderByExpiry(medicineId, warehouseId)
                .stream()
                .map(Batch::getQuantity)
                .filter(q -> q != null && q > 0)
                .reduce(0, Integer::sum);
    }

    private Order getIssueRequestEntity(Long id) {
        Order issueRequest = orderRepository.findIssueRequestById(id);
        if (issueRequest == null) {
            throw new RuntimeException("Issue request not found");
        }
        return issueRequest;
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderOrderId(order.getOrderId());
        int issuedQuantity = items.stream().map(OrderItem::getQuantity).filter(q -> q != null).reduce(0, Integer::sum);
        int approvedQuantity = order.getApprovedQuantity() == null ? 0 : order.getApprovedQuantity();
        int remainingQuantity = Math.max(approvedQuantity - issuedQuantity, 0);

        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getOrderId());
        response.setMedicineId(order.getMedicine() != null ? order.getMedicine().getMedicineId() : null);
        response.setMedicineName(order.getMedicine() != null ? order.getMedicine().getName() : null);
        response.setRequestedQuantity(order.getRequestedQuantity());
        response.setApprovedQuantity(order.getApprovedQuantity());
        response.setIssuedQuantity(issuedQuantity);
        response.setRemainingQuantity(remainingQuantity);
        response.setDepartment(order.getDepartment());
        response.setPurpose(order.getPurpose());
        response.setWarehouseId(order.getWarehouse() != null ? order.getWarehouse().getWarehouseId() : null);
        response.setWarehouseName(order.getWarehouse() != null ? order.getWarehouse().getName() : null);
        response.setRequestDate(order.getRequestDate());
        response.setNeededDate(order.getNeededDate());
        response.setStatus(order.getStatus() != null ? order.getStatus().name() : null);
        response.setRejectionReason(order.getRejectionReason());
        response.setCreatedById(order.getCreatedBy() != null ? order.getCreatedBy().getUserId() : null);
        response.setCreatedByName(resolveUserName(order.getCreatedBy()));
        response.setApprovedById(order.getApprovedBy() != null ? order.getApprovedBy().getUserId() : null);
        response.setApprovedByName(resolveUserName(order.getApprovedBy()));
        response.setApprovedAt(order.getApprovedAt());
        response.setCompletedAt(order.getCompletedAt());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        enrichStockInsights(
                response,
                order.getMedicine() != null ? order.getMedicine().getMedicineId() : null,
                order.getWarehouse() != null ? order.getWarehouse().getWarehouseId() : null,
                order.getRequestedQuantity());
        response.setItems(items.stream().map(this::toIssueHistoryDto).collect(Collectors.toList()));
        return response;
    }

    private void enrichStockInsights(OrderResponse response, Long medicineId, Long selectedWarehouseId, Integer requestedQuantity) {
        if (medicineId == null) {
            return;
        }

        int requested = requestedQuantity != null ? requestedQuantity : 0;
        List<Batch> allBatches = batchRepository.findByMedicine_MedicineId(medicineId);

        Map<Long, Integer> stockByWarehouse = new LinkedHashMap<>();
        Map<Long, String> warehouseNameById = new LinkedHashMap<>();

        for (Batch batch : allBatches) {
            if (batch.getWarehouse() == null || batch.getWarehouse().getWarehouseId() == null) {
                continue;
            }
            int qty = batch.getQuantity() != null ? batch.getQuantity() : 0;
            if (qty <= 0) {
                continue;
            }
            Long warehouseId = batch.getWarehouse().getWarehouseId();
            stockByWarehouse.merge(warehouseId, qty, Integer::sum);
            warehouseNameById.putIfAbsent(warehouseId, batch.getWarehouse().getName());
        }

        int selectedWarehouseStock = selectedWarehouseId != null ? stockByWarehouse.getOrDefault(selectedWarehouseId, 0) : 0;
        response.setAvailableStockInWarehouse(selectedWarehouseStock);
        response.setSuggestedAvailableQuantity(Math.min(requested, selectedWarehouseStock));

        List<OrderResponse.StockOption> alternatives = stockByWarehouse.entrySet().stream()
                .filter(entry -> selectedWarehouseId == null || !entry.getKey().equals(selectedWarehouseId))
                .map(entry -> new OrderResponse.StockOption(
                        entry.getKey(),
                        warehouseNameById.get(entry.getKey()),
                        entry.getValue()))
                .sorted(Comparator.comparing(OrderResponse.StockOption::getAvailableQuantity, Comparator.nullsFirst(Comparator.reverseOrder())))
                .limit(3)
                .collect(Collectors.toList());

        response.setAlternativeWarehouses(alternatives);
    }

    private OrderItemDTO toIssueHistoryDto(OrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setOrderItemId(item.getOrderItemId());
        dto.setOrderId(item.getOrder() != null ? item.getOrder().getOrderId() : null);
        dto.setMedicineId(item.getMedicine() != null ? item.getMedicine().getMedicineId() : null);
        dto.setMedicineName(item.getMedicine() != null ? item.getMedicine().getName() : null);
        dto.setBatchId(item.getBatch() != null ? item.getBatch().getBatchId() : null);
        dto.setLotNumber(item.getBatch() != null ? item.getBatch().getLotNumber() : null);
        dto.setExpiryDate(item.getBatch() != null ? item.getBatch().getExpiryDate() : null);
        dto.setQuantity(item.getQuantity());
        dto.setWarehouseId(item.getWarehouse() != null ? item.getWarehouse().getWarehouseId() : null);
        dto.setWarehouseName(item.getWarehouse() != null ? item.getWarehouse().getName() : null);
        dto.setIssuedById(item.getIssuedBy() != null ? item.getIssuedBy().getUserId() : null);
        dto.setIssuedByName(resolveUserName(item.getIssuedBy()));
        dto.setDepartment(item.getOrder() != null ? item.getOrder().getDepartment() : null);
        dto.setIssuedAt(item.getIssuedAt());
        return dto;
    }

    private IssueReportResponse.IssueReportItem toIssueReportItem(OrderItem item) {
        return IssueReportResponse.IssueReportItem.builder()
                .orderItemId(item.getOrderItemId())
                .requestId(item.getOrder() != null ? item.getOrder().getOrderId() : null)
                .medicineId(item.getMedicine() != null ? item.getMedicine().getMedicineId() : null)
                .medicineName(item.getMedicine() != null ? item.getMedicine().getName() : null)
                .batchId(item.getBatch() != null ? item.getBatch().getBatchId() : null)
                .lotNumber(item.getBatch() != null ? item.getBatch().getLotNumber() : null)
                .expiryDate(item.getBatch() != null ? item.getBatch().getExpiryDate() : null)
                .quantity(item.getQuantity())
                .warehouseId(item.getWarehouse() != null ? item.getWarehouse().getWarehouseId() : null)
                .warehouseName(item.getWarehouse() != null ? item.getWarehouse().getName() : null)
                .issuedById(item.getIssuedBy() != null ? item.getIssuedBy().getUserId() : null)
                .issuedByName(resolveUserName(item.getIssuedBy()))
                .department(item.getOrder() != null ? item.getOrder().getDepartment() : null)
                .issuedAt(item.getIssuedAt())
                .build();
    }

    private void validatePageAndSize(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be >= 0");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("Date range is invalid");
        }
    }

    private String resolveUserName(User user) {
        if (user == null) {
            return null;
        }
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        return user.getUsername();
    }
}
