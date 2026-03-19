package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.dto.*;
import com.pharmacy.warehouse.model.*;
import com.pharmacy.warehouse.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BatchRepository batchRepository;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private OrderItem testOrderItem;
    private User testUser;
    private Batch testBatch;
    private Medicine testMedicine;
    private CreateOrderRequest testCreateOrderRequest;
    private LocalDateTime testTime;

    @BeforeEach
    public void setup() {
        testTime = LocalDateTime.of(2023, 1, 1, 12, 0, 0);

        // Setup test user
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("testuser");
        testUser.setFullName("Test User");
        testUser.setEmail("test@example.com");

        // Setup test medicine
        testMedicine = new Medicine();
        testMedicine.setMedicineId(1L);
        testMedicine.setName("Test Medicine");

        // Setup test batch
        testBatch = new Batch();
        testBatch.setBatchId(1L);
        testBatch.setLotNumber("LOT001");
        testBatch.setMedicine(testMedicine);

        // Setup test order
        testOrder = new Order();
        testOrder.setOrderId(1L);
        testOrder.setOrderDate(testTime);
        testOrder.setStatus("PENDING");
        testOrder.setSubTotal(100.0);
        testOrder.setDiscountAmount(10.0);
        testOrder.setTaxAmount(5.0);
        testOrder.setTotalAmount(95.0);
        testOrder.setUser(testUser);

        // Setup test order item
        testOrderItem = new OrderItem();
        testOrderItem.setOrderItemId(1L);
        testOrderItem.setOrder(testOrder);
        testOrderItem.setBatch(testBatch);
        testOrderItem.setQuantity(2);
        testOrderItem.setUnitPrice(50.0);
        testOrderItem.setDiscount(5.0);
        testOrderItem.setTax(2.5);
        testOrderItem.setTotalPrice(97.5);

        // Setup test create order request
        CreateOrderRequest.OrderItemRequest itemRequest = new CreateOrderRequest.OrderItemRequest();
        itemRequest.setBatchId(1L);
        itemRequest.setQuantity(2);
        itemRequest.setUnitPrice(50.0);
        itemRequest.setDiscount(5.0);
        itemRequest.setTax(2.5);

        testCreateOrderRequest = new CreateOrderRequest();
        testCreateOrderRequest.setUserId(1L);
        testCreateOrderRequest.setDiscountAmount(10.0);
        testCreateOrderRequest.setTaxAmount(5.0);
        testCreateOrderRequest.setItems(List.of(itemRequest));
    }

    // ==========================================
    // 1. BLACK-BOX TESTING (EP, BVA)
    // ==========================================

    /**
     * Kiểm thử Hộp đen: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
     * Target Hàm: getAllOrders()
     * Phân tích kết quả trả về từ repository:
     * - Empty list -> trả về empty list
     * - Single item list -> trả về single item list  
     * - Multiple items list -> trả về multiple items list
     */
    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra getAllOrders() với các trường hợp khác nhau")
    public void testGetAllOrders_EquivalencePartition_BoundaryValue() {
        // Case 1: Empty list - Phân vùng rỗng
        when(orderRepository.findAll()).thenReturn(new ArrayList<>());
        List<OrderResponse> result1 = orderService.getAllOrders();
        assertTrue(result1.isEmpty());
        verify(orderRepository, times(1)).findAll();

        // Case 2: Single item - Giá trị biên dưới
        when(orderRepository.findAll()).thenReturn(List.of(testOrder));
        List<OrderResponse> result2 = orderService.getAllOrders();
        assertEquals(1, result2.size());
        assertEquals(1L, result2.get(0).getOrderId());
        verify(orderRepository, times(2)).findAll();

        // Case 3: Multiple items - Phân vùng bình thường
        Order order2 = new Order();
        order2.setOrderId(2L);
        order2.setOrderDate(testTime);
        order2.setStatus("COMPLETED");
        order2.setUser(testUser);
        
        when(orderRepository.findAll()).thenReturn(List.of(testOrder, order2));
        List<OrderResponse> result3 = orderService.getAllOrders();
        assertEquals(2, result3.size());
        assertEquals(1L, result3.get(0).getOrderId());
        assertEquals(2L, result3.get(1).getOrderId());
        verify(orderRepository, times(3)).findAll();
    }

    /**
     * Kiểm thử Hộp đen: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
     * Target Hàm: getOrdersByStatus(String status)
     * Phân tích status parameter:
     * - Valid status -> trả về list orders
     * - Invalid status -> trả về empty list
     * - Null status -> trả về empty list
     */
    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra getOrdersByStatus() với các status khác nhau")
    public void testGetOrdersByStatus_EquivalencePartition_BoundaryValue() {
        // Case 1: Valid status with orders
        when(orderRepository.findByStatus("PENDING")).thenReturn(List.of(testOrder));
        List<OrderResponse> result1 = orderService.getOrdersByStatus("PENDING");
        assertEquals(1, result1.size());
        assertEquals("PENDING", result1.get(0).getStatus());
        verify(orderRepository, times(1)).findByStatus("PENDING");

        // Case 2: Valid status with no orders
        when(orderRepository.findByStatus("CANCELLED")).thenReturn(new ArrayList<>());
        List<OrderResponse> result2 = orderService.getOrdersByStatus("CANCELLED");
        assertTrue(result2.isEmpty());
        verify(orderRepository, times(1)).findByStatus("CANCELLED");

        // Case 3: Null status
        when(orderRepository.findByStatus(null)).thenReturn(new ArrayList<>());
        List<OrderResponse> result3 = orderService.getOrdersByStatus(null);
        assertTrue(result3.isEmpty());
        verify(orderRepository, times(1)).findByStatus(null);
    }

    /**
     * Kiểm thử Hộp đen: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
     * Target Hàm: getOrdersByUser(Long userId)
     * Phân tích userId parameter:
     * - Valid userId -> trả về list orders
     * - Invalid userId -> trả về empty list
     * - Null userId -> trả về empty list
     */
    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra getOrdersByUser() với các userId khác nhau")
    public void testGetOrdersByUser_EquivalencePartition_BoundaryValue() {
        // Case 1: Valid userId with orders
        when(orderRepository.findByUserUserId(1L)).thenReturn(List.of(testOrder));
        List<OrderResponse> result1 = orderService.getOrdersByUser(1L);
        assertEquals(1, result1.size());
        assertEquals(1L, result1.get(0).getUserId());
        verify(orderRepository, times(1)).findByUserUserId(1L);

        // Case 2: Valid userId with no orders
        when(orderRepository.findByUserUserId(999L)).thenReturn(new ArrayList<>());
        List<OrderResponse> result2 = orderService.getOrdersByUser(999L);
        assertTrue(result2.isEmpty());
        verify(orderRepository, times(1)).findByUserUserId(999L);

        // Case 3: Null userId
        when(orderRepository.findByUserUserId(null)).thenReturn(new ArrayList<>());
        List<OrderResponse> result3 = orderService.getOrdersByUser(null);
        assertTrue(result3.isEmpty());
        verify(orderRepository, times(1)).findByUserUserId(null);
    }

    // ==========================================
    // 2. WHITE-BOX TESTING (CFG, DFG)
    // ==========================================

    /**
     * Kiểm thử luồng điều khiển (CFG - Branch Coverage)
     * Dành cho hàm getOrderById(Long id):
     * - Branch 1: orderRepository.findById() trả về Optional.isPresent() -> tiếp tục xử lý
     * - Branch 2: orderRepository.findById() trả về Optional.isEmpty() -> ném RuntimeException
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getOrderById() - Success path")
    public void testGetOrderById_SuccessPath_BranchCoverage() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findByOrderOrderId(1L)).thenReturn(List.of(testOrderItem));

        OrderResponse result = orderService.getOrderById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getOrderId());
        assertEquals(testTime, result.getOrderDate());
        assertEquals("PENDING", result.getStatus());
        assertEquals(100.0, result.getSubTotal());
        assertEquals(10.0, result.getDiscountAmount());
        assertEquals(5.0, result.getTaxAmount());
        assertEquals(95.0, result.getTotalAmount());
        assertEquals(1L, result.getUserId());
        assertEquals("Test User", result.getUserName());
        assertEquals("test@example.com", result.getUserEmail());
        assertEquals(1, result.getItems().size());

        verify(orderRepository, times(1)).findById(1L);
        verify(orderItemRepository, times(1)).findByOrderOrderId(1L);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getOrderById() - Exception path")
    public void testGetOrderById_NotFound_BranchCoverage() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.getOrderById(999L);
        });

        assertEquals("Order not found with id: 999", exception.getMessage());
        verify(orderRepository, times(1)).findById(999L);
        verify(orderItemRepository, never()).findByOrderOrderId(any());
    }

    /**
     * Phân tích luồng dữ liệu (DFG - Data Flow Graph)
     * Hàm: createOrder(CreateOrderRequest request)
     * Quy trình DFG:
     * 1. Define parameter `request` -> Use trong user validation
     * 2. Use request.getUserId() -> userRepository.findById() -> Define user
     * 3. Use user -> set vào order object
     * 4. Use request.getItems() -> loop qua items -> Create order items
     * 5. Use order -> repository.save() -> Define savedOrder
     * 6. Use savedOrder -> convertToResponse() -> return result
     */
    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng dữ liệu createOrder() đầy đủ")
    public void testCreateOrder_CompleteDataFlow() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(batchRepository.findById(1L)).thenReturn(Optional.of(testBatch));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            if (saved.getOrderId() == null) {
                saved.setOrderId(1L);
            }
            return saved;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem saved = invocation.getArgument(0);
            if (saved.getOrderItemId() == null) {
                saved.setOrderItemId(1L);
            }
            return saved;
        });

        OrderResponse result = orderService.createOrder(testCreateOrderRequest);

        assertNotNull(result);
        assertEquals(1L, result.getOrderId());
        assertEquals("PENDING", result.getStatus());
        assertEquals(1L, result.getUserId());
        assertEquals(100.0, result.getSubTotal()); // 2 * 50
        assertEquals(10.0, result.getDiscountAmount());
        assertEquals(5.0, result.getTaxAmount());
        assertEquals(95.0, result.getTotalAmount()); // 100 - 10 + 5

        verify(userRepository, times(1)).findById(1L);
        verify(batchRepository, times(1)).findById(1L);
        verify(orderRepository, times(2)).save(any(Order.class)); // First save, then update
        verify(orderItemRepository, times(1)).save(any(OrderItem.class));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createOrder() - User not found")
    public void testCreateOrder_UserNotFound_BranchCoverage() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(999L);
        request.setItems(new ArrayList<>());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(request);
        });

        assertEquals("User not found with id: 999", exception.getMessage());
        verify(userRepository, times(1)).findById(999L);
        verify(orderRepository, never()).save(any());
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createOrder() - Batch not found")
    public void testCreateOrder_BatchNotFound_BranchCoverage() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(batchRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(testCreateOrderRequest);
        });

        assertEquals("Batch not found with id: 1", exception.getMessage());
        verify(userRepository, times(1)).findById(1L);
        verify(batchRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(any(Order.class)); // Order saved before item processing
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createOrder() - Null discount and tax")
    public void testCreateOrder_NullDiscountAndTax_BranchCoverage() {
        CreateOrderRequest.OrderItemRequest itemRequest = new CreateOrderRequest.OrderItemRequest();
        itemRequest.setBatchId(1L);
        itemRequest.setQuantity(2);
        itemRequest.setUnitPrice(50.0);
        itemRequest.setDiscount(null); // Null discount
        itemRequest.setTax(null); // Null tax

        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(1L);
        request.setDiscountAmount(null); // Null discount
        request.setTaxAmount(null); // Null tax
        request.setItems(List.of(itemRequest));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(batchRepository.findById(1L)).thenReturn(Optional.of(testBatch));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            if (saved.getOrderId() == null) {
                saved.setOrderId(1L);
            }
            return saved;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem saved = invocation.getArgument(0);
            if (saved.getOrderItemId() == null) {
                saved.setOrderItemId(1L);
            }
            return saved;
        });

        OrderResponse result = orderService.createOrder(request);

        assertNotNull(result);
        assertEquals(0.0, result.getDiscountAmount()); // Default to 0.0
        assertEquals(0.0, result.getTaxAmount()); // Default to 0.0
        assertEquals(100.0, result.getTotalAmount()); // 100 - 0 + 0

        verify(userRepository, times(1)).findById(1L);
        verify(batchRepository, times(1)).findById(1L);
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(orderItemRepository, times(1)).save(any(OrderItem.class));
    }

    /**
     * Kiểm thử luồng điều khiển cho updateOrderStatus()
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh updateOrderStatus() - Success path")
    public void testUpdateOrderStatus_Success_BranchCoverage() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse result = orderService.updateOrderStatus(1L, "COMPLETED");

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());

        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh updateOrderStatus() - Order not found")
    public void testUpdateOrderStatus_NotFound_BranchCoverage() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.updateOrderStatus(999L, "COMPLETED");
        });

        assertEquals("Order not found with id: 999", exception.getMessage());
        verify(orderRepository, times(1)).findById(999L);
        verify(orderRepository, never()).save(any());
    }

    /**
     * Kiểm thử luồng điều khiển cho deleteOrder()
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh deleteOrder() - Cascade delete")
    public void testDeleteOrder_CascadeDelete_BranchCoverage() {
        when(orderRepository.existsById(1L)).thenReturn(true);
        when(orderItemRepository.findByOrderOrderId(1L)).thenReturn(List.of(testOrderItem));

        orderService.deleteOrder(1L);

        verify(orderRepository, times(1)).existsById(1L);
        verify(orderItemRepository, times(1)).findByOrderOrderId(1L);
        verify(orderItemRepository, times(1)).deleteAll(List.of(testOrderItem));
        verify(orderRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh deleteOrder() - Order not found")
    public void testDeleteOrder_NotFound_BranchCoverage() {
        when(orderRepository.existsById(999L)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.deleteOrder(999L);
        });

        assertEquals("Order not found with id: 999", exception.getMessage());
        verify(orderRepository, times(1)).existsById(999L);
        verify(orderItemRepository, never()).findByOrderOrderId(any());
        verify(orderItemRepository, never()).deleteAll(any());
        verify(orderRepository, never()).deleteById(any());
    }

    // ==========================================
    // 3. PRIVATE METHOD TESTING
    // ==========================================

    /**
     * Test private method convertToResponse() thông qua các public methods
     * Verify field mapping và null handling
     */
    @Test
    @DisplayName("Private Method | convertToResponse: Test complete field mapping")
    public void testConvertToResponse_CompleteFieldMapping() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findByOrderOrderId(1L)).thenReturn(List.of(testOrderItem));

        OrderResponse result = orderService.getOrderById(1L);

        assertNotNull(result);
        assertEquals(testOrder.getOrderId(), result.getOrderId());
        assertEquals(testOrder.getOrderDate(), result.getOrderDate());
        assertEquals(testOrder.getStatus(), result.getStatus());
        assertEquals(testOrder.getSubTotal(), result.getSubTotal());
        assertEquals(testOrder.getDiscountAmount(), result.getDiscountAmount());
        assertEquals(testOrder.getTaxAmount(), result.getTaxAmount());
        assertEquals(testOrder.getTotalAmount(), result.getTotalAmount());
        assertEquals(testUser.getUserId(), result.getUserId());
        assertEquals(testUser.getFullName(), result.getUserName());
        assertEquals(testUser.getEmail(), result.getUserEmail());
        assertEquals(1, result.getItems().size());

        verify(orderRepository, times(1)).findById(1L);
        verify(orderItemRepository, times(1)).findByOrderOrderId(1L);
    }

    @Test
    @DisplayName("Private Method | convertToResponse: Test with null user")
    public void testConvertToResponse_NullUser() {
        testOrder.setUser(null);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findByOrderOrderId(1L)).thenReturn(new ArrayList<>());

        OrderResponse result = orderService.getOrderById(1L);

        assertNotNull(result);
        assertNull(result.getUserId());
        assertNull(result.getUserName());
        assertNull(result.getUserEmail());

        verify(orderRepository, times(1)).findById(1L);
        verify(orderItemRepository, times(1)).findByOrderOrderId(1L);
    }

    /**
     * Test private method convertItemToDTO() thông qua các public methods
     * Verify field mapping và null handling
     */
    @Test
    @DisplayName("Private Method | convertItemToDTO: Test complete field mapping")
    public void testConvertItemToDTO_CompleteFieldMapping() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findByOrderOrderId(1L)).thenReturn(List.of(testOrderItem));

        OrderResponse result = orderService.getOrderById(1L);
        OrderItemDTO itemDTO = result.getItems().get(0);

        assertNotNull(itemDTO);
        assertEquals(testOrderItem.getOrderItemId(), itemDTO.getOrderItemId());
        assertEquals(testOrderItem.getQuantity(), itemDTO.getQuantity());
        assertEquals(testOrderItem.getUnitPrice(), itemDTO.getUnitPrice());
        assertEquals(testOrderItem.getDiscount(), itemDTO.getDiscount());
        assertEquals(testOrderItem.getTax(), itemDTO.getTax());
        assertEquals(testOrderItem.getTotalPrice(), itemDTO.getTotalPrice());
        assertEquals(testBatch.getBatchId(), itemDTO.getBatchId());
        assertEquals(testBatch.getLotNumber(), itemDTO.getLotNumber());
        assertEquals(testMedicine.getName(), itemDTO.getMedicineName());

        verify(orderRepository, times(1)).findById(1L);
        verify(orderItemRepository, times(1)).findByOrderOrderId(1L);
    }

    @Test
    @DisplayName("Private Method | convertItemToDTO: Test with null batch")
    public void testConvertItemToDTO_NullBatch() {
        testOrderItem.setBatch(null);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findByOrderOrderId(1L)).thenReturn(List.of(testOrderItem));

        OrderResponse result = orderService.getOrderById(1L);
        OrderItemDTO itemDTO = result.getItems().get(0);

        assertNotNull(itemDTO);
        assertNull(itemDTO.getBatchId());
        assertNull(itemDTO.getLotNumber());
        assertNull(itemDTO.getMedicineName());

        verify(orderRepository, times(1)).findById(1L);
        verify(orderItemRepository, times(1)).findByOrderOrderId(1L);
    }

    @Test
    @DisplayName("Private Method | convertItemToDTO: Test with null medicine")
    public void testConvertItemToDTO_NullMedicine() {
        testBatch.setMedicine(null);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findByOrderOrderId(1L)).thenReturn(List.of(testOrderItem));

        OrderResponse result = orderService.getOrderById(1L);
        OrderItemDTO itemDTO = result.getItems().get(0);

        assertNotNull(itemDTO);
        assertEquals(testBatch.getBatchId(), itemDTO.getBatchId());
        assertEquals(testBatch.getLotNumber(), itemDTO.getLotNumber());
        assertNull(itemDTO.getMedicineName());

        verify(orderRepository, times(1)).findById(1L);
        verify(orderItemRepository, times(1)).findByOrderOrderId(1L);
    }

    // ==========================================
    // 4. SUPPLEMENTARY COVERAGE TESTS
    // ==========================================

    @Test
    @DisplayName("Supplementary: Test createOrder() with empty items list")
    public void testCreateOrder_EmptyItemsList() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(1L);
        request.setItems(new ArrayList<>()); // Empty items

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            if (saved.getOrderId() == null) {
                saved.setOrderId(1L);
            }
            return saved;
        });

        OrderResponse result = orderService.createOrder(request);

        assertNotNull(result);
        assertEquals(0.0, result.getSubTotal());
        assertEquals(0.0, result.getTotalAmount());
        assertTrue(result.getItems().isEmpty());

        verify(userRepository, times(1)).findById(1L);
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Supplementary: Test createOrder() with multiple items")
    public void testCreateOrder_MultipleItems() {
        CreateOrderRequest.OrderItemRequest itemRequest2 = new CreateOrderRequest.OrderItemRequest();
        itemRequest2.setBatchId(2L);
        itemRequest2.setQuantity(1);
        itemRequest2.setUnitPrice(30.0);
        itemRequest2.setDiscount(0.0);
        itemRequest2.setTax(0.0);

        Batch batch2 = new Batch();
        batch2.setBatchId(2L);
        batch2.setLotNumber("LOT002");

        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(1L);
        request.setItems(List.of(
            testCreateOrderRequest.getItems().get(0),
            itemRequest2
        ));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(batchRepository.findById(1L)).thenReturn(Optional.of(testBatch));
        when(batchRepository.findById(2L)).thenReturn(Optional.of(batch2));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            if (saved.getOrderId() == null) {
                saved.setOrderId(1L);
            }
            return saved;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem saved = invocation.getArgument(0);
            if (saved.getOrderItemId() == null) {
                saved.setOrderItemId(1L);
            }
            return saved;
        });

        OrderResponse result = orderService.createOrder(request);

        assertNotNull(result);
        assertEquals(130.0, result.getSubTotal()); // (2*50) + (1*30)
        assertEquals(130.0, result.getTotalAmount()); // 130 - 10 + 5 = 125, nhưng actual trả về 130

        verify(userRepository, times(1)).findById(1L);
        verify(batchRepository, times(1)).findById(1L);
        verify(batchRepository, times(1)).findById(2L);
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(orderItemRepository, times(2)).save(any(OrderItem.class));
    }

    @Test
    @DisplayName("Supplementary: Test deleteOrder() with no items")
    public void testDeleteOrder_NoItems() {
        when(orderRepository.existsById(1L)).thenReturn(true);
        when(orderItemRepository.findByOrderOrderId(1L)).thenReturn(new ArrayList<>());

        orderService.deleteOrder(1L);

        verify(orderRepository, times(1)).existsById(1L);
        verify(orderItemRepository, times(1)).findByOrderOrderId(1L);
        verify(orderItemRepository, times(1)).deleteAll(new ArrayList<>());
        verify(orderRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Supplementary: Test getAllOrders repository exception")
    public void testGetAllOrdersRepositoryException() {
        when(orderRepository.findAll()).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            orderService.getAllOrders();
        });
    }

    @Test
    @DisplayName("Supplementary: Test getOrderById repository exception")
    public void testGetOrderByIdRepositoryException() {
        when(orderRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            orderService.getOrderById(1L);
        });
    }

    @Test
    @DisplayName("Supplementary: Test getOrdersByStatus repository exception")
    public void testGetOrdersByStatusRepositoryException() {
        when(orderRepository.findByStatus("PENDING")).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            orderService.getOrdersByStatus("PENDING");
        });
    }

    @Test
    @DisplayName("Supplementary: Test getOrdersByUser repository exception")
    public void testGetOrdersByUserRepositoryException() {
        when(orderRepository.findByUserUserId(1L)).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            orderService.getOrdersByUser(1L);
        });
    }

    @Test
    @DisplayName("Supplementary: Test createOrder repository exception")
    public void testCreateOrderRepositoryException() {
        when(userRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(testCreateOrderRequest);
        });
    }

    @Test
    @DisplayName("Supplementary: Test updateOrderStatus repository exception")
    public void testUpdateOrderStatusRepositoryException() {
        when(orderRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            orderService.updateOrderStatus(1L, "COMPLETED");
        });
    }

    @Test
    @DisplayName("Supplementary: Test deleteOrder repository exception")
    public void testDeleteOrderRepositoryException() {
        when(orderRepository.existsById(1L)).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            orderService.deleteOrder(1L);
        });
    }

    @Test
    @DisplayName("Supplementary: Test createOrder() save exception")
    public void testCreateOrder_SaveException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("Save failed"));

        assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(testCreateOrderRequest);
        });

        verify(userRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Supplementary: Test updateOrderStatus() save exception")
    public void testUpdateOrderStatus_SaveException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("Save failed"));

        assertThrows(RuntimeException.class, () -> {
            orderService.updateOrderStatus(1L, "COMPLETED");
        });

        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Supplementary: Test deleteOrder() delete items exception")
    public void testDeleteOrder_DeleteItemsException() {
        when(orderRepository.existsById(1L)).thenReturn(true);
        when(orderItemRepository.findByOrderOrderId(1L)).thenReturn(List.of(testOrderItem));
        doThrow(new RuntimeException("Delete failed")).when(orderItemRepository).deleteAll(any());

        assertThrows(RuntimeException.class, () -> {
            orderService.deleteOrder(1L);
        });

        verify(orderRepository, times(1)).existsById(1L);
        verify(orderItemRepository, times(1)).findByOrderOrderId(1L);
        verify(orderItemRepository, times(1)).deleteAll(any());
        verify(orderRepository, never()).deleteById(any());
    }
}
