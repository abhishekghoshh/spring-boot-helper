package com.commercemesh.order.controller;

import com.commercemesh.order.dto.CreateOrderRequest;
import com.commercemesh.order.dto.OrderItemRequest;
import com.commercemesh.order.dto.OrderResponse;
import com.commercemesh.order.dto.OrderStatusUpdateRequest;
import com.commercemesh.order.dto.PageResponse;
import com.commercemesh.order.dto.ShippingAddress;
import com.commercemesh.order.model.OrderStatus;
import com.commercemesh.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@DisplayName("OrderController")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    private OrderResponse sampleOrderResponse;

    @BeforeEach
    void setUp() {
        sampleOrderResponse = new OrderResponse();
        sampleOrderResponse.setId(1L);
        sampleOrderResponse.setOrderNumber("ORD-001");
        sampleOrderResponse.setUserId(100L);
        sampleOrderResponse.setStatus(OrderStatus.PENDING);
        sampleOrderResponse.setSubtotal(new BigDecimal("99.98"));
        sampleOrderResponse.setTax(new BigDecimal("7.9984"));
        sampleOrderResponse.setShippingCost(BigDecimal.ZERO);
        sampleOrderResponse.setTotal(new BigDecimal("107.9784"));
        sampleOrderResponse.setItems(List.of());
        sampleOrderResponse.setShippingAddress(new ShippingAddress(
                "123 Main St", "Apt 4B", "New York", "NY", "US", "10001", "555-1234"));
    }

    @Nested
    @DisplayName("POST /api/v1/orders")
    @WithMockUser(username = "testuser", roles = {"CUSTOMER"})
    class CreateOrderTests {

        @Test
        @DisplayName("should return 201 and OrderResponse when order is created")
        void createOrderValidReturns201() throws Exception {
            OrderItemRequest item = new OrderItemRequest(1L, "Product", new BigDecimal("49.99"), 2);
            ShippingAddress addr = new ShippingAddress("123 Main St", "Apt 4B", "NY", "NY", "US", "10001", "555-1234");
            CreateOrderRequest request = new CreateOrderRequest(List.of(item), addr, "notes");

            when(orderService.createOrder(any(), any(CreateOrderRequest.class)))
                    .thenReturn(sampleOrderResponse);

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.orderNumber").value("ORD-001"))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.total").value(107.9784));
        }

        @Test
        @DisplayName("should return 400 when items list is empty")
        void createOrderWithEmptyItemsReturns400() throws Exception {
            ShippingAddress addr = new ShippingAddress("123 Main St", "Apt 4B", "NY", "NY", "US", "10001", "555-1234");
            CreateOrderRequest request = new CreateOrderRequest(List.of(), addr, "notes");

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when shipping address is null")
        void createOrderWithNullShippingAddressReturns400() throws Exception {
            OrderItemRequest item = new OrderItemRequest(1L, "Product", new BigDecimal("49.99"), 2);
            CreateOrderRequest request = new CreateOrderRequest(List.of(item), null, "notes");

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated user tries to create order")
        @WithAnonymousUser
        void createOrderUnauthenticatedReturns401() throws Exception {
            OrderItemRequest item = new OrderItemRequest(1L, "Product", new BigDecimal("49.99"), 2);
            ShippingAddress addr = new ShippingAddress("123 Main St", "Apt 4B", "NY", "NY", "US", "10001", "555-1234");
            CreateOrderRequest request = new CreateOrderRequest(List.of(item), addr, "notes");

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orders")
    @WithMockUser(username = "testuser", roles = {"CUSTOMER"})
    class GetUserOrdersTests {

        @Test
        @DisplayName("should return 200 and paginated list of orders")
        void getUserOrdersReturnsPaginatedList() throws Exception {
            PageResponse<OrderResponse> pageResponse = new PageResponse<>(
                    List.of(sampleOrderResponse), 0, 20, 1, 1, true, true);

            when(orderService.getOrdersByUser(any(), any()))
                    .thenReturn(pageResponse);

            mockMvc.perform(get("/api/v1/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.first").value(true))
                    .andExpect(jsonPath("$.last").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orders/{id}")
    @WithMockUser(username = "testuser", roles = {"CUSTOMER"})
    class GetOrderByIdTests {

        @Test
        @DisplayName("should return 200 and order when found")
        void getOrderByIdReturns200() throws Exception {
            when(orderService.getOrderById(1L)).thenReturn(sampleOrderResponse);

            mockMvc.perform(get("/api/v1/orders/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.orderNumber").value("ORD-001"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/orders/{id}/status")
    class UpdateOrderStatusTests {

        @Test
        @DisplayName("should return 200 when ADMIN updates status")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void updateStatusAsAdminReturns200() throws Exception {
            OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.CONFIRMED, null);
            OrderResponse confirmedResponse = new OrderResponse();
            confirmedResponse.setId(1L);
            confirmedResponse.setStatus(OrderStatus.CONFIRMED);
            confirmedResponse.setUserId(100L);

            when(orderService.updateOrderStatus(eq(1L), any(OrderStatusUpdateRequest.class)))
                    .thenReturn(confirmedResponse);

            mockMvc.perform(put("/api/v1/orders/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));
        }

        @Test
        @DisplayName("should return 200 when ORDER_MANAGER updates status")
        @WithMockUser(username = "ordermanager", roles = {"ORDER_MANAGER"})
        void updateStatusAsOrderManagerReturns200() throws Exception {
            OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.PROCESSING, null);
            OrderResponse processingResponse = new OrderResponse();
            processingResponse.setId(1L);
            processingResponse.setStatus(OrderStatus.PROCESSING);
            processingResponse.setUserId(100L);

            when(orderService.updateOrderStatus(eq(1L), any(OrderStatusUpdateRequest.class)))
                    .thenReturn(processingResponse);

            mockMvc.perform(put("/api/v1/orders/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PROCESSING"));
        }

        @Test
        @DisplayName("should return 403 when CUSTOMER tries to update status")
        @WithMockUser(username = "customer", roles = {"CUSTOMER"})
        void updateStatusAsCustomerReturns403() throws Exception {
            OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.CONFIRMED, null);

            mockMvc.perform(put("/api/v1/orders/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }
}
