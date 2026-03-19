package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.model.Medicine;
import com.pharmacy.warehouse.repository.MedicineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Expression;

@ExtendWith(MockitoExtension.class)
public class MedicineServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @InjectMocks
    private MedicineService medicineService;

    private Medicine medicineMock;

    @BeforeEach
    public void setup() {
        medicineMock = new Medicine();
        medicineMock.setMedicineId(1L);
        medicineMock.setName("Paracetamol");
        medicineMock.setManufacturer("Pfizer");
        medicineMock.setStorageCondition("Normal");
        medicineMock.setDescription("Common pain reliever");
    }

    // ==========================================
    // 1. BLACK-BOX TESTING (EP, BVA, DT)
    // ==========================================

    /**
     * Kiểm thử Hộp đen: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
     * Target Hàm: getMedicines (page, size)
     * Phân tích size:
     * Logic normalize: size <= 0 ? 10 : Math.min(size, 100);
     * - size = -1  => size = 10
     * - size = 0   => size = 10
     * - size = 1   => size = 1
     * - size = 99  => size = 99
     * - size = 100 => size = 100
     * - size = 101 => size = 100
     */
    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra chuẩn hoá biến size")
    public void testGetMedicines_SizeNormalization_BVA() {
        // Cấu hình mock
        Page<Medicine> mockPage = new PageImpl<>(List.of(medicineMock));
        when(medicineRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        // Bắt Pageable truyền vào repository
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        // Case 1: size <= 0 (-1) => Trở về mặc định 10
        medicineService.getMedicines(0, -1, "", "", "", "", "asc");
        Mockito.verify(medicineRepository, Mockito.atLeastOnce()).findAll(any(Specification.class), captor.capture());
        assertEquals(10, captor.getValue().getPageSize());

        // Case 2: size <= 0 (0) => Trở về mặc định 10
        medicineService.getMedicines(0, 0, "", "", "", "", "asc");
        Mockito.verify(medicineRepository, Mockito.atLeastOnce()).findAll(any(Specification.class), captor.capture());
        assertEquals(10, captor.getValue().getPageSize());

        // Case 3: size hơp lệ (1) => giữ nguyên 1
        medicineService.getMedicines(0, 1, "", "", "", "", "asc");
        Mockito.verify(medicineRepository, Mockito.atLeastOnce()).findAll(any(Specification.class), captor.capture());
        assertEquals(1, captor.getValue().getPageSize());

        // Case 4: size hơp lệ (99) => giữ nguyên 99
        medicineService.getMedicines(0, 99, "", "", "", "", "asc");
        Mockito.verify(medicineRepository, Mockito.atLeastOnce()).findAll(any(Specification.class), captor.capture());
        assertEquals(99, captor.getValue().getPageSize());

        // Case 5: size max (100) => giữ nguyên 100
        medicineService.getMedicines(0, 100, "", "", "", "", "asc");
        Mockito.verify(medicineRepository, Mockito.atLeastOnce()).findAll(any(Specification.class), captor.capture());
        assertEquals(100, captor.getValue().getPageSize());

        // Case 6: size > max (101) => chặn lại ở mức 100
        medicineService.getMedicines(0, 101, "", "", "", "", "asc");
        Mockito.verify(medicineRepository, Mockito.atLeastOnce()).findAll(any(Specification.class), captor.capture());
        assertEquals(100, captor.getValue().getPageSize());
    }

    /**
     * Bảng quyết định (Decision Table - DT)
     * Logic Filter của getMedicines:
     * - Có search ? Yes/No
     * - Có manufacturer ? Yes/No ("all" là bằng No)
     * - Có storageCondition ? Yes/No ("all" là bằng No)
     * 3 biến = 8 rules. Ở đây chỉ lấy ra một vài luồng để minh hoạ DT:
     * - R1: F, F, F -> Truy vấn không có điều kiện nào (Specification rỗng)
     * - R2: T, T, T -> Truy vấn có 3 Predicates gộp lại.
     */
    @Test
    @DisplayName("Black-Box | DT: Kiểm thử tổ hợp các bộ lọc tìm kiếm")
    public void testGetMedicines_Filter_DT() {
        Page<Medicine> mockPage = new PageImpl<>(new ArrayList<>());
        when(medicineRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<Medicine>> specCaptor = ArgumentCaptor.forClass(Specification.class);

        // R1: Không có tiêu chí lọc. search=null, manufacturer="all", storage="all"
        medicineService.getMedicines(0, 10, null, "all", "all", "", "");
        Mockito.verify(medicineRepository, Mockito.atLeastOnce()).findAll(specCaptor.capture(), any(Pageable.class));
        assertNotNull(specCaptor.getValue());

        // R2: Có cả 3 tiêu chí lọc
        medicineService.getMedicines(0, 10, "Para", "Pfizer", "Cold", "", "");
        Mockito.verify(medicineRepository, Mockito.atLeastOnce()).findAll(specCaptor.capture(), any(Pageable.class));
        assertNotNull(specCaptor.getValue());
    }

    // ==========================================
    // 2. WHITE-BOX TESTING (CFG, DFG)
    // ==========================================

    /**
     * Kiểm thử luồng điều khiển (CFG - Branch Coverage)
     * Dành cho khối lệnh `switch (sortBy)`:
     *     case "manufacturer" -> "manufacturer";
     *     case "storageCondition" -> "storageCondition";
     *     default -> "name";
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh cho cấu trúc Switch-Case sortBy")
    public void testGetMedicines_SwitchCoverage_CFG() {
        Page<Medicine> mockPage = new PageImpl<>(List.of(medicineMock));
        when(medicineRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        // Nhánh 1: sortBy = manufacturer
        medicineService.getMedicines(0, 10, "", "", "", "manufacturer", "asc");
        Mockito.verify(medicineRepository, Mockito.atLeastOnce()).findAll(any(Specification.class), captor.capture());
        Sort sort1 = captor.getValue().getSort();
        assertTrue(sort1.getOrderFor("manufacturer") != null);

        // Nhánh 2: sortBy = storageCondition
        medicineService.getMedicines(0, 10, "", "", "", "storageCondition", "asc");
        Mockito.verify(medicineRepository, Mockito.atLeastOnce()).findAll(any(Specification.class), captor.capture());
        Sort sort2 = captor.getValue().getSort();
        assertTrue(sort2.getOrderFor("storageCondition") != null);

        // Nhánh 3: default -> name
        medicineService.getMedicines(0, 10, "", "", "", "unknown_field", "asc");
        Mockito.verify(medicineRepository, Mockito.atLeastOnce()).findAll(any(Specification.class), captor.capture());
        Sort sort3 = captor.getValue().getSort();
        assertTrue(sort3.getOrderFor("name") != null);
    }

    /**
     * Phân tích luồng dữ liệu (DFG - Data Flow Graph)
     * Hàm: update(Long id, Medicine data)
     * Quy trình DFG:
     * 1. Define `m` (từ DB)
     * 2. Use `data` để set cho `m`
     * 3. Use `m` (đẩy vào save())
     */
    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng cập nhật dữ liệu (Update)")
    public void testUpdate_DataFlowGraph_DFG() {
        Long id = 1L;
        Medicine newData = new Medicine();
        newData.setName("Panadol");
        newData.setManufacturer("GSK");
        newData.setStorageCondition("Cool");
        newData.setDescription("New description");

        when(medicineRepository.findById(id)).thenReturn(Optional.of(medicineMock));
        when(medicineRepository.save(any(Medicine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Thực thi
        Medicine updatedResult = medicineService.update(id, newData);

        // Kểm duyệt luồng Data đã chảy thành công từ newData -> m -> save
        assertEquals("Panadol", updatedResult.getName());
        assertEquals("GSK", updatedResult.getManufacturer());
        assertEquals("Cool", updatedResult.getStorageCondition());
        assertEquals("New description", updatedResult.getDescription());
    }

    @Test
    @DisplayName("Basic Test: Thêm dữ liệu")
    public void testCreate() {
        when(medicineRepository.save(any(Medicine.class))).thenReturn(medicineMock);
        Medicine created = medicineService.create(medicineMock);
        assertNotNull(created);
        assertEquals(medicineMock.getName(), created.getName());
    }

    @Test
    @DisplayName("Basic Test: Xóa dữ liệu")
    public void testDelete() {
        Mockito.doNothing().when(medicineRepository).deleteById(1L);
        medicineService.delete(1L);
        Mockito.verify(medicineRepository, Mockito.times(1)).deleteById(1L);
    }

    // ==========================================
    // 3. SUPPLEMENTARY COVERAGE TESTS
    // ==========================================

    @Test
    @DisplayName("Supplementary: Test Sort Direction DESC")
    public void testGetMedicines_SortDesc() {
        Page<Medicine> mockPage = new PageImpl<>(List.of(medicineMock));
        when(medicineRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        medicineService.getMedicines(0, 10, "", "", "", "name", "desc");
        Mockito.verify(medicineRepository, Mockito.atLeastOnce()).findAll(any(Specification.class), captor.capture());
        
        Sort sort = captor.getValue().getSort();
        Sort.Order order = sort.getOrderFor("name");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    @DisplayName("Supplementary: Test getById Exception Path")
    public void testGetById_NotFound() {
        when(medicineRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            medicineService.getById(999L);
        });

        assertEquals("Medicine not found", exception.getMessage());
    }

    @Test
    @DisplayName("Supplementary: Test getById Found Path")
    public void testGetById_Found() {
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(medicineMock));

        Medicine result = medicineService.getById(1L);

        assertNotNull(result);
        assertEquals("Paracetamol", result.getName());
    }

    @Test
    @DisplayName("Supplementary: Test Specification with partial filters")
    public void testGetMedicines_PartialFilters() {
        Page<Medicine> mockPage = new PageImpl<>(new ArrayList<>());
        when(medicineRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        
        // Test manufacturer only
        medicineService.getMedicines(0, 10, null, "Pfizer", "all", "name", "asc");
        
        // Test storage condition only
        medicineService.getMedicines(0, 10, null, "all", "Normal", "name", "asc");

        // Verify findAll was called 2 more times
        Mockito.verify(medicineRepository, Mockito.times(2)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Supplementary: Test Specification Lambda (toPredicate)")
    public void testSpecificationLambda_ToPredicate() {
        // Setup mocks for the JPA Criteria API
        Root<Medicine> root = Mockito.mock(Root.class);
        CriteriaQuery<?> query = Mockito.mock(CriteriaQuery.class);
        CriteriaBuilder builder = Mockito.mock(CriteriaBuilder.class);
        
        // Setup paths
        Path<String> namePath = Mockito.mock(Path.class);
        Path<String> manufacturerPath = Mockito.mock(Path.class);
        Path<String> storagePath = Mockito.mock(Path.class);
        
        when(root.<String>get("name")).thenReturn(namePath);
        when(root.<String>get("manufacturer")).thenReturn(manufacturerPath);
        when(root.<String>get("storageCondition")).thenReturn(storagePath);
        
        // Setup expression and predicate mocks
        Expression<String> upperName = Mockito.mock(Expression.class);
        Expression<String> upperManufacturer = Mockito.mock(Expression.class);
        Expression<String> upperStorage = Mockito.mock(Expression.class);
        
        Predicate likePredicate = Mockito.mock(Predicate.class);
        Predicate equalMfgPredicate = Mockito.mock(Predicate.class);
        Predicate equalStoragePredicate = Mockito.mock(Predicate.class);
        Predicate andPredicate = Mockito.mock(Predicate.class);
        
        // Return expressions when builder.lower is called
        when(builder.lower(namePath)).thenReturn(upperName);
        when(builder.lower(manufacturerPath)).thenReturn(upperManufacturer);
        when(builder.lower(storagePath)).thenReturn(upperStorage);
        
        // Return specific predicates
        when(builder.like(upperName, "%para%")).thenReturn(likePredicate);
        when(builder.equal(upperManufacturer, "pfizer")).thenReturn(equalMfgPredicate);
        when(builder.equal(upperStorage, "cold")).thenReturn(equalStoragePredicate);
        
        // Capture the predicates array passed to builder.and
        ArgumentCaptor<Predicate[]> predicatesCaptor = ArgumentCaptor.forClass(Predicate[].class);
        when(builder.and(predicatesCaptor.capture())).thenReturn(andPredicate);
        
        // We need to capture the specification to call toPredicate on it manually
        Page<Medicine> mockPage = new PageImpl<>(List.of(medicineMock));
        when(medicineRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        ArgumentCaptor<Specification<Medicine>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        
        // Trigger the service method with all 3 filters to hit all branched in lambda
        medicineService.getMedicines(0, 10, "Para", "Pfizer", "Cold", "name", "asc");
        
        Mockito.verify(medicineRepository).findAll(specCaptor.capture(), any(Pageable.class));
        Specification<Medicine> spec = specCaptor.getValue();
        
        // Act: Manually call the lambda's toPredicate
        Predicate finalPredicate = spec.toPredicate(root, query, builder);
        
        // Assert
        assertNotNull(finalPredicate);
        assertEquals(andPredicate, finalPredicate);
        
        Predicate[] capturedPredicates = predicatesCaptor.getValue();
        assertEquals(3, capturedPredicates.length);
        assertEquals(likePredicate, capturedPredicates[0]);
        assertEquals(equalMfgPredicate, capturedPredicates[1]);
        assertEquals(equalStoragePredicate, capturedPredicates[2]);
    }
}
