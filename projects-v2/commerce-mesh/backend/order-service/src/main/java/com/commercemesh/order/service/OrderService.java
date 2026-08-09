package com.commercemesh.order.service;

import com.commercemesh.order.client.InventoryClient;
import com.commercemesh.order.client.InventoryClient.StockReservationRequest;
import com.commercemesh.order.client.PaymentClient;
import com.commercemesh.order.client.PaymentClient.PaymentRequest;
import com.commercemesh.order.dto.*;
import com.commercemesh.order.exception.InsufficientStockException;
import com.commercemesh.order.exception.OrderStatusTransitionException;
import com.commercemesh.order.exception.ResourceNotFoundException;
import com.commercemesh.order.model.*;
import com.commercemesh.order.repository.OrderEventRepository;
import com.commercemesh.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private static final Set<OrderStatus> CANCELLABLE_STATUSES = Set.of(OrderStatus.PENDING, OrderStatus.CONFIRMED);

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final OrderEventPublisher eventPublisher;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;

    public OrderService(OrderRepository orderRepository,
                        OrderEventRepository orderEventRepository,
                        OrderEventPublisher eventPublisher,
                        InventoryClient inventoryClient,
                        PaymentClient paymentClient) {
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
        this.eventPublisher = eventPublisher;
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
    }

    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        log.info("Creating order for user {}: {} items", userId, request.getItems().size());

        // Validate stock for each item
        for (OrderItemRequest item : request.getItems()) {
            validateAndReserveStock(item, userId);
        }

        // Build order
        Order order = new Order();
        order.setUserId(userId);
        order.setShippingAddress(toAddressEntity(request.getShippingAddress()));
        order.setNotes(request.getNotes());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);

        for (OrderItemRequest item : request.getItems()) {
            OrderItem orderItem = new OrderItem(
                    item.getProductId(),
                    item.getProductName(),
                    item.getPrice(),
                    item.getQuantity()
            );
            order.addItem(orderItem);
        }

        order.recalculate();

        Order savedOrder = orderRepository.save(order);
        log.info("Order created: id={}, orderNumber={}", savedOrder.getId(), savedOrder.getOrderNumber());

        // Save event
        saveOrderEvent(savedOrder.getId(), "ORDER_CREATED", savedOrder.getOrderNumber(), userId);

        // Publish event
        eventPublisher.publishOrderCreated(savedOrder.getId(), savedOrder.getOrderNumber(), userId);

        return toOrderResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        return toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrdersByUser(Long userId, Pageable pageable) {
        Page<Order> page = orderRepository.findByUserId(userId, pageable);
        Page<OrderResponse> responsePage = page.map(this::toOrderResponse);
        return PageResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        return toOrderResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        validateStatusTransition(order.getStatus(), request.getStatus());

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(request.getStatus());

        if (request.getTrackingNumber() != null && !request.getTrackingNumber().isBlank()) {
            order.setTrackingNumber(request.getTrackingNumber());
        }

        Order savedOrder = orderRepository.save(order);

        // Save event and publish based on new status
        String eventType = mapStatusToEventType(request.getStatus());
        saveOrderEvent(savedOrder.getId(), eventType, savedOrder.getOrderNumber(), savedOrder.getUserId());

        publishEventForStatus(savedOrder, request.getStatus());

        log.info("Order {} status updated: {} -> {}", orderId, oldStatus, request.getStatus());
        return toOrderResponse(savedOrder);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only cancel your own orders");
        }

        if (!CANCELLABLE_STATUSES.contains(order.getStatus())) {
            throw new OrderStatusTransitionException(
                    order.getStatus(),
                    OrderStatus.CANCELLED
            );
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        saveOrderEvent(savedOrder.getId(), "ORDER_CANCELLED", savedOrder.getOrderNumber(), userId);
        eventPublisher.publishOrderCancelled(savedOrder.getId(), savedOrder.getOrderNumber(), userId);

        log.info("Order {} cancelled by user {}", orderId, userId);
        return toOrderResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderEventResponse> getOrderHistory(Long orderId) {
        // Verify order exists
        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Order", "id", orderId);
        }

        return orderEventRepository.findByOrderIdOrderByCreatedAtAsc(orderId)
                .stream()
                .map(this::toOrderEventResponse)
                .collect(Collectors.toList());
    }

    // --- Private helpers ---

    private void validateAndReserveStock(OrderItemRequest item, Long userId) {
        try {
            Map<String, Object> stockResponse = inventoryClient.checkStock(item.getProductId());
            if (stockResponse == null || !Boolean.TRUE.equals(stockResponse.get("available"))) {
                int available = stockResponse != null && stockResponse.get("quantity") instanceof Number qty
                        ? qty.intValue() : 0;
                throw new InsufficientStockException(item.getProductId(), item.getQuantity(), available);
            }
            int available = stockResponse.get("quantity") instanceof Number qty ? qty.intValue() : 0;
            if (available < item.getQuantity()) {
                throw new InsufficientStockException(item.getProductId(), item.getQuantity(), available);
            }
            // Reserve stock
            inventoryClient.reserveStock(new StockReservationRequest(
                    item.getProductId(), item.getQuantity(), "user-" + userId));
        } catch (InsufficientStockException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Inventory service unavailable for product {}: {}", item.getProductId(), e.getMessage());
            // Proceed if inventory service is unavailable (resilience)
        }
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus target) {
        if (current == target) {
            return; // no-op
        }
        boolean valid = switch (current) {
            case PENDING -> target == OrderStatus.CONFIRMED || target == OrderStatus.CANCELLED;
            case CONFIRMED -> target == OrderStatus.PROCESSING || target == OrderStatus.CANCELLED;
            case PROCESSING -> target == OrderStatus.SHIPPED;
            case SHIPPED -> target == OrderStatus.DELIVERED;
            case DELIVERED -> target == OrderStatus.REFUNDED;
            case CANCELLED, REFUNDED -> false; // terminal states
        };
        if (!valid) {
            throw new OrderStatusTransitionException(current, target);
        }
    }

    private String mapStatusToEventType(OrderStatus status) {
        return switch (status) {
            case CONFIRMED -> "ORDER_CONFIRMED";
            case PROCESSING -> "ORDER_PROCESSING";
            case SHIPPED -> "ORDER_SHIPPED";
            case DELIVERED -> "ORDER_DELIVERED";
            case CANCELLED -> "ORDER_CANCELLED";
            default -> "ORDER_UPDATED";
        };
    }

    private void publishEventForStatus(Order order, OrderStatus status) {
        switch (status) {
            case CONFIRMED -> eventPublisher.publishOrderConfirmed(order.getId(), order.getOrderNumber(), order.getUserId());
            case SHIPPED -> eventPublisher.publishOrderShipped(order.getId(), order.getOrderNumber(),
                    order.getUserId(), order.getTrackingNumber());
            case DELIVERED -> eventPublisher.publishOrderDelivered(order.getId(), order.getOrderNumber(), order.getUserId());
            case CANCELLED -> eventPublisher.publishOrderCancelled(order.getId(), order.getOrderNumber(), order.getUserId());
            default -> { /* No event for other transitions */ }
        }
    }

    private void saveOrderEvent(Long orderId, String eventType, String orderNumber, Long userId) {
        String payload = buildEventPayload(orderId, orderNumber, userId, eventType);
        OrderEvent event = new OrderEvent(orderId, eventType, payload);
        orderEventRepository.save(event);
    }

    private String buildEventPayload(Long orderId, String orderNumber, Long userId, String eventType) {
        return String.format(
                "{\"orderId\":%d,\"orderNumber\":\"%s\",\"userId\":%d,\"eventType\":\"%s\"}",
                orderId, orderNumber, userId, eventType
        );
    }

    private OrderResponse toOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setUserId(order.getUserId());
        response.setStatus(order.getStatus());
        response.setSubtotal(order.getSubtotal());
        response.setTax(order.getTax());
        response.setShippingCost(order.getShippingCost());
        response.setTotal(order.getTotal());
        response.setPaymentId(order.getPaymentId());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setTrackingNumber(order.getTrackingNumber());
        response.setNotes(order.getNotes());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        if (order.getShippingAddress() != null) {
            response.setShippingAddress(new ShippingAddress(
                    order.getShippingAddress().getAddressLine1(),
                    order.getShippingAddress().getAddressLine2(),
                    order.getShippingAddress().getCity(),
                    order.getShippingAddress().getState(),
                    order.getShippingAddress().getCountry(),
                    order.getShippingAddress().getZipCode(),
                    order.getShippingAddress().getPhone()
            ));
        }

        if (order.getItems() != null) {
            response.setItems(order.getItems().stream()
                    .map(item -> new OrderItemResponse(
                            item.getId(),
                            item.getProductId(),
                            item.getProductName(),
                            item.getPrice(),
                            item.getQuantity(),
                            item.getSubtotal()
                    ))
                    .collect(Collectors.toList()));
        }

        return response;
    }

    private OrderEventResponse toOrderEventResponse(OrderEvent event) {
        return new OrderEventResponse(
                event.getId(),
                event.getOrderId(),
                event.getEventType(),
                event.getPayload(),
                event.getCreatedAt()
        );
    }

    private Address toAddressEntity(ShippingAddress dto) {
        return new Address(
                dto.addressLine1(),
                dto.addressLine2(),
                dto.city(),
                dto.state(),
                dto.country(),
                dto.zipCode(),
                dto.phone()
        );
    }
}
