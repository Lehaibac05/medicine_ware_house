package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.model.Medicine;
import com.pharmacy.warehouse.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
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

    private static final int DEFAULT_REORDER_LEVEL = 10;

    private final MedicineRepository medicineRepository;

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
        normalizeReorderLevel(medicine);
        return medicineRepository.save(medicine);
    }

    public Medicine update(Long id, Medicine data) {
        Medicine m = getById(id);
        m.setName(normalizeRequiredText(data.getName(), "Medicine name is required"));
        m.setManufacturer(normalizeRequiredText(data.getManufacturer(), "Manufacturer is required"));
        m.setStorageCondition(normalizeRequiredText(data.getStorageCondition(), "Storage condition is required"));
        m.setDescription(normalizeRequiredText(data.getDescription(), "Description is required"));
        m.setReorderLevel(resolveReorderLevel(data.getReorderLevel(), m.getReorderLevel()));
        return medicineRepository.save(m);
    }

    public void delete(Long id) {
        medicineRepository.deleteById(id);
    }

    private void normalizeReorderLevel(Medicine medicine) {
        medicine.setReorderLevel(resolveReorderLevel(medicine.getReorderLevel(), null));
    }

    private void normalizeTextFields(Medicine medicine) {
        medicine.setName(normalizeRequiredText(medicine.getName(), "Medicine name is required"));
        medicine.setManufacturer(normalizeRequiredText(medicine.getManufacturer(), "Manufacturer is required"));
        medicine.setStorageCondition(normalizeRequiredText(medicine.getStorageCondition(), "Storage condition is required"));
        medicine.setDescription(normalizeRequiredText(medicine.getDescription(), "Description is required"));
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

    private Integer resolveReorderLevel(Integer candidate, Integer fallback) {
        if (candidate != null && candidate >= 0) {
            return candidate;
        }
        if (fallback != null && fallback >= 0) {
            return fallback;
        }
        return DEFAULT_REORDER_LEVEL;
    }
}
