package com.pharmacy.warehouse.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pharmacy.warehouse.dto.ExpiringBatchResponse;
import com.pharmacy.warehouse.dto.InventoryDetailResponse;
import com.pharmacy.warehouse.dto.InventoryResponse;
import com.pharmacy.warehouse.dto.InventorySummaryResponse;
import com.pharmacy.warehouse.model.Batch;
import com.pharmacy.warehouse.model.Medicine;
import com.pharmacy.warehouse.repository.BatchRepository;
import com.pharmacy.warehouse.repository.MedicineRepository;
import com.pharmacy.warehouse.repository.WarehouseRepository;
import com.pharmacy.warehouse.repository.projection.InventoryAggregateProjection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

        private static final int DEFAULT_REORDER_LEVEL = 10;
    private static final int EXPIRING_SOON_DAYS = 30;

    private final BatchRepository batchRepository;
    private final MedicineRepository medicineRepository;
    private final WarehouseRepository warehouseRepository;

    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventory(
            String medicineName,
            Long warehouseId,
            LocalDate expiryFrom,
            LocalDate expiryTo,
            String status) {
        log.info("Fetching inventory list with filters medicineName={}, warehouseId={}, status={}",
                medicineName, warehouseId, status);

        if (warehouseId != null && !warehouseRepository.existsById(warehouseId)) {
            throw new RuntimeException("Warehouse not found with id: " + warehouseId);
        }

        List<InventoryResponse> items = batchRepository
                .aggregateInventory(medicineName, warehouseId, expiryFrom, expiryTo)
                .stream()
                .map(this::toInventoryResponse)
                .collect(Collectors.toList());

        if (status == null || status.isBlank()) {
            return items;
        }

        String normalizedStatus = status.trim().toUpperCase();
        return items.stream()
                .filter(item -> normalizedStatus.equals(item.getStatus()))
                .collect(Collectors.toList());
    }

        @Transactional(readOnly = true)
        public Page<InventoryResponse> getInventoryPaged(
                        int page,
                        int size,
                        String medicineName,
                        Long warehouseId,
                        LocalDate expiryFrom,
                        LocalDate expiryTo,
                        String status) {
                int normalizedPage = Math.max(0, page);
                int normalizedSize = size <= 0 ? 10 : Math.min(size, 100);

                List<InventoryResponse> all = getInventory(medicineName, warehouseId, expiryFrom, expiryTo, status);
                int fromIndex = Math.min(normalizedPage * normalizedSize, all.size());
                int toIndex = Math.min(fromIndex + normalizedSize, all.size());

                return new PageImpl<>(
                                all.subList(fromIndex, toIndex),
                                org.springframework.data.domain.PageRequest.of(normalizedPage, normalizedSize),
                                all.size());
        }

    @Transactional(readOnly = true)
    public List<InventoryDetailResponse> getInventoryDetailByMedicine(Long medicineId) {
        log.info("Calculating stock details for medicine {}", medicineId);

        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new RuntimeException("Medicine not found with id: " + medicineId));

        List<Batch> batches = batchRepository.findInventoryDetailsByMedicine(medicineId);
        if (batches.isEmpty()) {
            return List.of();
        }

        Map<Long, List<Batch>> groupedByWarehouse = new LinkedHashMap<>();
        for (Batch batch : batches) {
            groupedByWarehouse
                    .computeIfAbsent(batch.getWarehouse().getWarehouseId(), key -> new ArrayList<>())
                    .add(batch);
        }

        List<InventoryDetailResponse> details = new ArrayList<>();

        for (List<Batch> warehouseBatches : groupedByWarehouse.values()) {
            Batch first = warehouseBatches.get(0);
            long totalStock = warehouseBatches.stream()
                    .map(Batch::getQuantity)
                    .filter(Objects::nonNull)
                    .mapToLong(Integer::longValue)
                    .sum();

            List<InventoryDetailResponse.BatchInfo> batchInfos = warehouseBatches.stream()
                    .map(batch -> InventoryDetailResponse.BatchInfo.builder()
                            .batchId(batch.getBatchId())
                            .lotNumber(batch.getLotNumber())
                            .quantity(batch.getQuantity())
                            .manufactureDate(batch.getManufactureDate())
                            .expiryDate(batch.getExpiryDate())
                            .build())
                    .collect(Collectors.toList());

            details.add(InventoryDetailResponse.builder()
                    .medicineId(medicine.getMedicineId())
                    .medicineName(medicine.getName())
                    .warehouseId(first.getWarehouse().getWarehouseId())
                    .warehouseName(first.getWarehouse().getName())
                    .totalStock(totalStock)
                    .batches(batchInfos)
                    .build());
        }

        return details;
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStockInventory() {
        log.info("Fetching low stock inventory list");
        return getInventory(null, null, null, null, "LOW_STOCK");
    }

    @Transactional(readOnly = true)
    public List<ExpiringBatchResponse> getExpiringBatches() {
        log.info("Fetching expiring batches within {} days", EXPIRING_SOON_DAYS);

        LocalDate threshold = LocalDate.now().plusDays(EXPIRING_SOON_DAYS);
        return batchRepository.findExpiringBatches(threshold).stream()
                .map(batch -> ExpiringBatchResponse.builder()
                        .batchId(batch.getBatchId())
                        .lotNumber(batch.getLotNumber())
                        .quantity(batch.getQuantity())
                        .manufactureDate(batch.getManufactureDate())
                        .expiryDate(batch.getExpiryDate())
                        .medicineId(batch.getMedicine().getMedicineId())
                        .medicineName(batch.getMedicine().getName())
                        .warehouseId(batch.getWarehouse().getWarehouseId())
                        .warehouseName(batch.getWarehouse().getName())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventorySummaryResponse getInventorySummary() {
        log.info("Fetching inventory summary");

        List<InventoryResponse> inventory = getInventory(null, null, null, null, null);
        long totalMedicines = inventory.stream()
                .map(InventoryResponse::getMedicineId)
                .distinct()
                .count();
        long lowStockCount = inventory.stream()
                .filter(item -> "LOW_STOCK".equals(item.getStatus()))
                .count();
        long expiringSoonCount = inventory.stream()
                .filter(item -> "EXPIRING_SOON".equals(item.getStatus()))
                .count();

        return InventorySummaryResponse.builder()
                .totalMedicines(totalMedicines)
                .totalBatches(batchRepository.count())
                .lowStockCount(lowStockCount)
                .expiringSoonCount(expiringSoonCount)
                .build();
    }

    private InventoryResponse toInventoryResponse(InventoryAggregateProjection projection) {
        String status = determineStatus(
                projection.getTotalStock(),
                projection.getNearestExpiryDate(),
                projection.getReorderLevel());

        return InventoryResponse.builder()
                .medicineId(projection.getMedicineId())
                .medicineName(projection.getMedicineName())
                .warehouseId(projection.getWarehouseId())
                .warehouseName(projection.getWarehouseName())
                .totalStock(projection.getTotalStock())
                .batchCount(projection.getBatchCount())
                .nearestExpiryDate(projection.getNearestExpiryDate())
                .status(status)
                .build();
    }

        private String determineStatus(Long totalStock, LocalDate nearestExpiryDate, Integer reorderLevel) {
        long stock = totalStock == null ? 0L : totalStock;
                int threshold = (reorderLevel == null || reorderLevel < 0)
                                ? DEFAULT_REORDER_LEVEL
                                : reorderLevel;

                if (stock < threshold) {
            return "LOW_STOCK";
        }

        if (nearestExpiryDate != null) {
            LocalDate now = LocalDate.now();
                        LocalDate expiryThreshold = now.plusDays(EXPIRING_SOON_DAYS);
                        if (!nearestExpiryDate.isBefore(now) && !nearestExpiryDate.isAfter(expiryThreshold)) {
                return "EXPIRING_SOON";
            }
        }

        return "NORMAL";
    }
}
