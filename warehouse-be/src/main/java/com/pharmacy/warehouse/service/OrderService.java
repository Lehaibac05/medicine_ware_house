package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.dto.*;
import com.pharmacy.warehouse.model.Batch;
import com.pharmacy.warehouse.model.Order;
import com.pharmacy.warehouse.model.OrderItem;
import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final BatchRepository batchRepository;

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        return convertToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(String status) {
        return orderRepository.findByStatus(status).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(Long userId) {
        return orderRepository.findByUserUserId(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating new order for user: {}", request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));

        Order order = new Order();
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PENDING");
        order.setUser(user);
        order.setDiscountAmount(request.getDiscountAmount() != null ? request.getDiscountAmount() : 0.0);
        order.setTaxAmount(request.getTaxAmount() != null ? request.getTaxAmount() : 0.0);

        // Calculate totals
        double subTotal = 0.0;
        
        // Save order first to get ID
        order = orderRepository.save(order);
        
        // Create order items
        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Batch batch = batchRepository.findById(itemReq.getBatchId())
                    .orElseThrow(() -> new RuntimeException("Batch not found with id: " + itemReq.getBatchId()));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setBatch(batch);
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(itemReq.getUnitPrice());
            item.setDiscount(itemReq.getDiscount() != null ? itemReq.getDiscount() : 0.0);
            item.setTax(itemReq.getTax() != null ? itemReq.getTax() : 0.0);
            
            double itemTotal = (itemReq.getQuantity() * itemReq.getUnitPrice()) 
                               - item.getDiscount() + item.getTax();
            item.setTotalPrice(itemTotal);
            
            orderItemRepository.save(item);
            
            subTotal += (itemReq.getQuantity() * itemReq.getUnitPrice());
        }

        // Update order totals
        order.setSubTotal(subTotal);
        double totalAmount = subTotal - order.getDiscountAmount() + order.getTaxAmount();
        order.setTotalAmount(totalAmount);
        
        order = orderRepository.save(order);

        log.info("Order created successfully with id: {}", order.getOrderId());
        return convertToResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, String status) {
        log.info("Updating order {} status to: {}", id, status);
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        
        order.setStatus(status);
        order = orderRepository.save(order);
        
        return convertToResponse(order);
    }

    @Transactional
    public void deleteOrder(Long id) {
        log.info("Deleting order with id: {}", id);
        
        if (!orderRepository.existsById(id)) {
            throw new RuntimeException("Order not found with id: " + id);
        }
        
        // Delete order items first
        List<OrderItem> items = orderItemRepository.findByOrderOrderId(id);
        orderItemRepository.deleteAll(items);
        
        // Then delete order
        orderRepository.deleteById(id);
    }

    private OrderResponse convertToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getOrderId());
        response.setOrderDate(order.getOrderDate());
        response.setStatus(order.getStatus());
        response.setSubTotal(order.getSubTotal());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setTaxAmount(order.getTaxAmount());
        response.setTotalAmount(order.getTotalAmount());
        
        if (order.getUser() != null) {
            response.setUserId(order.getUser().getUserId());
            response.setUserName(order.getUser().getFullName());
            response.setUserEmail(order.getUser().getEmail());
        }
        
        // Get order items
        List<OrderItem> items = orderItemRepository.findByOrderOrderId(order.getOrderId());
        List<OrderItemDTO> itemDTOs = items.stream()
                .map(this::convertItemToDTO)
                .collect(Collectors.toList());
        response.setItems(itemDTOs);
        
        return response;
    }

    private OrderItemDTO convertItemToDTO(OrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setOrderItemId(item.getOrderItemId());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setDiscount(item.getDiscount());
        dto.setTax(item.getTax());
        dto.setTotalPrice(item.getTotalPrice());
        
        if (item.getBatch() != null) {
            dto.setBatchId(item.getBatch().getBatchId());
            dto.setLotNumber(item.getBatch().getLotNumber());
            
            if (item.getBatch().getMedicine() != null) {
                dto.setMedicineName(item.getBatch().getMedicine().getName());
            }
        }
        
        return dto;
    }
}
