package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.model.Medicine;
import com.pharmacy.warehouse.repository.BatchRepository;
import com.pharmacy.warehouse.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final BatchRepository batchRepository;
    private final SettingsService settingsService;

    public Page<Medicine> getMedicines(
            int page,
            int size,
            String search,
            String manufacturer,
            String storageCondition,
            String sortBy,
            String sortDir
    ) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = size <= 0 ? 10 : Math.min(size, 100);

        String sortableField = switch (sortBy) {
            case "manufacturer" -> "manufacturer";
            case "storageCondition" -> "storageCondition";
            default -> "name";
        };

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(
                normalizedPage,
                normalizedSize,
                Sort.by(direction, sortableField)
        );

        Specification<Medicine> spec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String keyword = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.like(builder.lower(root.get("name")), keyword));
            }

            if (StringUtils.hasText(manufacturer) && !"all".equalsIgnoreCase(manufacturer)) {
                predicates.add(
                        builder.equal(
                                builder.lower(root.get("manufacturer")),
                                manufacturer.trim().toLowerCase(Locale.ROOT)
                        )
                );
            }

            if (StringUtils.hasText(storageCondition) && !"all".equalsIgnoreCase(storageCondition)) {
                predicates.add(
                        builder.equal(
                                builder.lower(root.get("storageCondition")),
                                storageCondition.trim().toLowerCase(Locale.ROOT)
                        )
                );
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };

        return medicineRepository.findAll(spec, pageable);
    }

    public Medicine getById(Long id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medicine not found"));
    }

    public Medicine create(Medicine medicine) {
        normalizeTextFields(medicine);
        validateDuplicateName(medicine.getName(), null);
        normalizeReorderLevel(medicine);
        return medicineRepository.save(medicine);
    }

    public Medicine update(Long id, Medicine data) {
        Medicine m = getById(id);
        String normalizedName = normalizeRequiredText(data.getName(), "Medicine name is required");
        validateDuplicateName(normalizedName, id);

        m.setName(normalizedName);
        m.setManufacturer(normalizeRequiredText(data.getManufacturer(), "Manufacturer is required"));
        m.setStorageCondition(normalizeRequiredText(data.getStorageCondition(), "Storage condition is required"));
        m.setDescription(normalizeOptionalText(data.getDescription()));
        m.setReorderLevel(resolveReorderLevel(data.getReorderLevel(), m.getReorderLevel()));
        return medicineRepository.save(m);
    }

    public void delete(Long id) {
        if (!medicineRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy thuốc để xóa");
        }

        if (batchRepository.existsByMedicine_MedicineId(id)) {
            throw new IllegalStateException("Không thể xóa thuốc vì đã tồn tại lô thuốc liên quan");
        }

        try {
            medicineRepository.deleteById(id);
        } catch (EmptyResultDataAccessException ex) {
            throw new RuntimeException("Không tìm thấy thuốc để xóa");
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Không thể xóa thuốc vì đang có dữ liệu liên quan", ex);
        }
    }

    private void normalizeReorderLevel(Medicine medicine) {
        medicine.setReorderLevel(resolveReorderLevel(medicine.getReorderLevel(), null));
    }

    private void normalizeTextFields(Medicine medicine) {
        medicine.setName(normalizeRequiredText(medicine.getName(), "Medicine name is required"));
        medicine.setManufacturer(normalizeRequiredText(medicine.getManufacturer(), "Manufacturer is required"));
        medicine.setStorageCondition(normalizeRequiredText(medicine.getStorageCondition(), "Storage condition is required"));
        medicine.setDescription(normalizeOptionalText(medicine.getDescription()));
    }

    private String normalizeRequiredText(String value, String errorMessage) {
        if (value == null) {
            throw new IllegalArgumentException(errorMessage);
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }

        return trimmed;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return "";
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? "" : trimmed;
    }

    private void validateDuplicateName(String normalizedName, Long currentMedicineId) {
        boolean existed = currentMedicineId == null
                ? medicineRepository.existsByNameIgnoreCase(normalizedName)
                : medicineRepository.existsByNameIgnoreCaseAndMedicineIdNot(normalizedName, currentMedicineId);

        if (existed) {
            throw new IllegalArgumentException("Tên thuốc đã tồn tại");
        }
    }

    private Integer resolveReorderLevel(Integer candidate, Integer fallback) {
        if (candidate != null && candidate >= 0) {
            return candidate;
        }
        if (fallback != null && fallback >= 0) {
            return fallback;
        }
        return settingsService.getDefaultReorderLevel();
    }
}
