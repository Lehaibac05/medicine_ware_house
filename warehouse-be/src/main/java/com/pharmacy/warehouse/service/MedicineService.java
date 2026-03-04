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
        return medicineRepository.save(medicine);
    }

    public Medicine update(Long id, Medicine data) {
        Medicine m = getById(id);
        m.setName(data.getName());
        m.setManufacturer(data.getManufacturer());
        m.setStorageCondition(data.getStorageCondition());
        m.setDescription(data.getDescription());
        return medicineRepository.save(m);
    }

    public void delete(Long id) {
        medicineRepository.deleteById(id);
    }
}
