package com.commercemesh.inventory.controller;

import com.commercemesh.inventory.dto.InventoryRequest;
import com.commercemesh.inventory.dto.InventoryResponse;
import com.commercemesh.inventory.dto.StockCheckResponse;
import com.commercemesh.inventory.dto.StockReservationRequest;
import com.commercemesh.inventory.dto.StockReservationResponse;
import com.commercemesh.inventory.entity.ReservationStatus;
import com.commercemesh.inventory.service.InventoryService;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
@DisplayName("InventoryController")
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryService inventoryService;

    private InventoryResponse sampleInventoryResponse;
    private StockReservationResponse sampleReservationResponse;

    @BeforeEach
    void setUp() {
        sampleInventoryResponse = new InventoryResponse(
                1L, 100L, 10L, 50, 5, 45, 10, 50,
                LocalDate.of(2025, 1, 15),
                Instant.now(), Instant.now());

        sampleReservationResponse = new StockReservationResponse(
                1L, 100L, 10L, 200L, 10,
                ReservationStatus.RESERVED,
                Instant.now().plusSeconds(1800),
                Instant.now());
    }

    @Nested
    @DisplayName("GET /api/v1/inventory/{productId}")
    class GetInventoryTests {

        @Test
        @DisplayName("should return 200 and stock info for a given product")
        @WithAnonymousUser
        void getInventoryReturnsStockInfo() throws Exception {
            when(inventoryService.getInventory(100L)).thenReturn(List.of(sampleInventoryResponse));

            mockMvc.perform(get("/api/v1/inventory/100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].productId").value(100))
                    .andExpect(jsonPath("$[0].warehouseId").value(10))
                    .andExpect(jsonPath("$[0].quantity").value(50))
                    .andExpect(jsonPath("$[0].reservedQuantity").value(5))
                    .andExpect(jsonPath("$[0].availableQuantity").value(45));
        }

        @Test
        @DisplayName("should return 200 and empty list for unknown product")
        @WithAnonymousUser
        void getInventoryReturnsEmptyList() throws Exception {
            when(inventoryService.getInventory(999L)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/inventory/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/inventory/check")
    class CheckStockTests {

        @Test
        @DisplayName("should return 200 and stock check response")
        @WithAnonymousUser
        void checkStockReturnsResponse() throws Exception {
            StockCheckResponse checkResponse = new StockCheckResponse(true, "Stock available: 50 units");
            when(inventoryService.checkStockDetailed(eq(100L), anyInt())).thenReturn(checkResponse);

            mockMvc.perform(get("/api/v1/inventory/check")
                            .param("productId", "100")
                            .param("quantity", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.available").value(true))
                    .andExpect(jsonPath("$.message").value("Stock available: 50 units"));
        }

        @Test
        @DisplayName("should return 400 when productId is missing")
        @WithAnonymousUser
        void checkStockWithoutProductIdReturns400() throws Exception {
            mockMvc.perform(get("/api/v1/inventory/check")
                            .param("quantity", "10"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when quantity is missing or zero")
        @WithAnonymousUser
        void checkStockWithZeroQuantityReturns400() throws Exception {
            mockMvc.perform(get("/api/v1/inventory/check")
                            .param("productId", "100")
                            .param("quantity", "0"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/inventory")
    class CreateInventoryTests {

        @Test
        @DisplayName("should return 201 when INVENTORY_MANAGER creates inventory")
        @WithMockUser(username = "invmanager", roles = {"INVENTORY_MANAGER"})
        void createInventoryReturns201() throws Exception {
            InventoryRequest request = new InventoryRequest(100L, 10L, 50, 10, 100);
            when(inventoryService.createInventory(any(InventoryRequest.class)))
                    .thenReturn(sampleInventoryResponse);

            mockMvc.perform(post("/api/v1/inventory")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.productId").value(100))
                    .andExpect(jsonPath("$.quantity").value(50));
        }

        @Test
        @DisplayName("should return 403 when regular user tries to create inventory")
        @WithMockUser(username = "regular", roles = {"CUSTOMER"})
        void createInventoryAsCustomerReturns403() throws Exception {
            InventoryRequest request = new InventoryRequest(100L, 10L, 50, 10, 100);

            mockMvc.perform(post("/api/v1/inventory")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated user tries to create inventory")
        @WithAnonymousUser
        void createInventoryUnauthenticatedReturns401() throws Exception {
            InventoryRequest request = new InventoryRequest(100L, 10L, 50, 10, 100);

            mockMvc.perform(post("/api/v1/inventory")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 400 when required fields are missing")
        @WithMockUser(username = "invmanager", roles = {"INVENTORY_MANAGER"})
        void createInventoryWithMissingFieldsReturns400() throws Exception {
            String invalidBody = """
                    {"productId":null,"warehouseId":10,"quantity":50,"reorderLevel":10,"reorderQuantity":100}""";

            mockMvc.perform(post("/api/v1/inventory")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/inventory/reserve")
    class ReserveStockTests {

        @Test
        @DisplayName("should return 201 and reservation when stock is reserved")
        @WithMockUser(username = "invmanager", roles = {"INVENTORY_MANAGER"})
        void reserveStockReturns201() throws Exception {
            StockReservationRequest request = new StockReservationRequest(100L, 10L, 200L, 10);
            when(inventoryService.reserveStock(any(StockReservationRequest.class)))
                    .thenReturn(sampleReservationResponse);

            mockMvc.perform(post("/api/v1/inventory/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.productId").value(100))
                    .andExpect(jsonPath("$.orderId").value(200))
                    .andExpect(jsonPath("$.quantity").value(10))
                    .andExpect(jsonPath("$.status").value("RESERVED"));
        }

        @Test
        @DisplayName("should return 403 when CUSTOMER tries to reserve stock")
        @WithMockUser(username = "customer", roles = {"CUSTOMER"})
        void reserveStockAsCustomerReturns403() throws Exception {
            StockReservationRequest request = new StockReservationRequest(100L, 10L, 200L, 10);

            mockMvc.perform(post("/api/v1/inventory/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 400 when quantity is less than 1")
        @WithMockUser(username = "invmanager", roles = {"INVENTORY_MANAGER"})
        void reserveStockWithInvalidQuantityReturns400() throws Exception {
            String invalidBody = """
                    {"productId":100,"warehouseId":10,"orderId":200,"quantity":0}""";

            mockMvc.perform(post("/api/v1/inventory/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/inventory/reservations/{orderId}/confirm")
    class ConfirmReservationTests {

        @Test
        @DisplayName("should return 200 when reservation is confirmed")
        @WithMockUser(username = "invmanager", roles = {"INVENTORY_MANAGER"})
        void confirmReservationReturns200() throws Exception {
            StockReservationResponse confirmedResponse = new StockReservationResponse(
                    1L, 100L, 10L, 200L, 10,
                    ReservationStatus.CONFIRMED,
                    Instant.now().plusSeconds(1800),
                    Instant.now());
            when(inventoryService.confirmReservation(200L)).thenReturn(confirmedResponse);

            mockMvc.perform(post("/api/v1/inventory/reservations/200/confirm"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(200))
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));
        }

        @Test
        @DisplayName("should return 403 when CUSTOMER tries to confirm reservation")
        @WithMockUser(username = "customer", roles = {"CUSTOMER"})
        void confirmReservationAsCustomerReturns403() throws Exception {
            mockMvc.perform(post("/api/v1/inventory/reservations/200/confirm"))
                    .andExpect(status().isForbidden());
        }
    }
}
