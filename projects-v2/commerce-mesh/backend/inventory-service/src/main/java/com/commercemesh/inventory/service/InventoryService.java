package com.commercemesh.inventory.service;

import com.commercemesh.inventory.dto.InventoryRequest;
import com.commercemesh.inventory.dto.InventoryResponse;
import com.commercemesh.inventory.dto.StockCheckResponse;
import com.commercemesh.inventory.dto.StockReservationRequest;
import com.commercemesh.inventory.dto.StockReservationResponse;
import com.commercemesh.inventory.entity.Inventory;
import com.commercemesh.inventory.entity.ReservationStatus;
import com.commercemesh.inventory.entity.StockReservation;
import com.commercemesh.inventory.exception.InsufficientStockException;
import com.commercemesh.inventory.exception.ResourceNotFoundException;
import com.commercemesh.inventory.repository.InventoryRepository;
import com.commercemesh.inventory.repository.StockReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository reservationRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                            StockReservationRepository reservationRepository) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
    }

    // --- Inventory CRUD ---

    public List<InventoryResponse> getInventory(Long productId) {
        return inventoryRepository.findByProductId(productId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public InventoryResponse getInventoryByWarehouse(Long productId, Long warehouseId) {
        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory",
                        "productId + warehouseId", productId + ", " + warehouseId));
        return toResponse(inventory);
    }

    public InventoryResponse createInventory(InventoryRequest request) {
        // Check if record already exists
        if (inventoryRepository.findByProductIdAndWarehouseId(request.productId(), request.warehouseId()).isPresent()) {
            throw new IllegalArgumentException(
                    "Inventory record already exists for product " + request.productId() +
                    " in warehouse " + request.warehouseId());
        }

        Inventory inventory = new Inventory(
                request.productId(),
                request.warehouseId(),
                request.quantity(),
                request.reorderLevel(),
                request.reorderQuantity()
        );

        if (request.quantity() > 0) {
            inventory.setLastRestockDate(LocalDate.now());
        }

        Inventory saved = inventoryRepository.save(inventory);
        return toResponse(saved);
    }

    public InventoryResponse updateInventory(Long productId, Long warehouseId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory",
                        "productId + warehouseId", productId + ", " + warehouseId));

        inventory.setQuantity(quantity);
        if (quantity > 0) {
            inventory.setLastRestockDate(LocalDate.now());
        }

        Inventory saved = inventoryRepository.save(inventory);
        return toResponse(saved);
    }

    // --- Stock Availability ---

    public boolean checkStock(Long productId, int quantity) {
        int available = inventoryRepository.getTotalAvailableQuantityByProductId(productId);
        return available >= quantity;
    }

    public StockCheckResponse checkStockDetailed(Long productId, int quantity) {
        int available = inventoryRepository.getTotalAvailableQuantityByProductId(productId);
        if (available >= quantity) {
            return new StockCheckResponse(true, "Stock available: " + available + " units");
        } else {
            return new StockCheckResponse(false,
                    "Insufficient stock: requested " + quantity + ", available " + available);
        }
    }

    // --- Reservations ---

    public StockReservationResponse reserveStock(StockReservationRequest request) {
        Long productId = request.productId();
        Long warehouseId = request.warehouseId();
        int quantity = request.quantity();

        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory",
                        "productId + warehouseId", productId + ", " + warehouseId));

        int available = inventory.getAvailableQuantity();
        if (available < quantity) {
            throw new InsufficientStockException(productId, quantity, available);
        }

        // Increment reserved quantity
        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        inventoryRepository.save(inventory);

        // Create reservation record (reserve for 30 minutes by default)
        Instant reservedUntil = Instant.now().plus(30, ChronoUnit.MINUTES);
        StockReservation reservation = new StockReservation(
                productId, warehouseId, request.orderId(), quantity, reservedUntil);

        StockReservation saved = reservationRepository.save(reservation);
        log.info("Stock reserved: productId={}, warehouseId={}, orderId={}, quantity={}",
                productId, warehouseId, request.orderId(), quantity);

        return toReservationResponse(saved);
    }

    public StockReservationResponse confirmReservation(Long orderId) {
        List<StockReservation> reservations = reservationRepository.findByOrderId(orderId);

        if (reservations.isEmpty()) {
            throw new ResourceNotFoundException("StockReservation", "orderId", orderId);
        }

        // Confirm all reservations for this order
        for (StockReservation reservation : reservations) {
            if (reservation.getStatus() != ReservationStatus.RESERVED) {
                throw new IllegalStateException(
                        "Reservation " + reservation.getId() + " is not in RESERVED state (current: " +
                        reservation.getStatus() + ")");
            }

            reservation.setStatus(ReservationStatus.CONFIRMED);

            // Decrement actual inventory quantity
            Inventory inventory = inventoryRepository
                    .findByProductIdAndWarehouseId(reservation.getProductId(), reservation.getWarehouseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory",
                            "productId + warehouseId",
                            reservation.getProductId() + ", " + reservation.getWarehouseId()));

            inventory.setQuantity(inventory.getQuantity() - reservation.getQuantity());
            inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
            inventoryRepository.save(inventory);
            reservationRepository.save(reservation);
        }

        log.info("Reservations confirmed for orderId={}, count={}", orderId, reservations.size());

        // Return the first reservation response (order-level confirmation)
        return toReservationResponse(reservations.get(0));
    }

    public StockReservationResponse cancelReservation(Long orderId) {
        List<StockReservation> reservations = reservationRepository.findByOrderId(orderId);

        if (reservations.isEmpty()) {
            throw new ResourceNotFoundException("StockReservation", "orderId", orderId);
        }

        for (StockReservation reservation : reservations) {
            if (reservation.getStatus() == ReservationStatus.CANCELLED ||
                reservation.getStatus() == ReservationStatus.EXPIRED) {
                continue; // already cancelled/expired
            }

            // Only release reserved quantity if the reservation was in RESERVED state
            if (reservation.getStatus() == ReservationStatus.RESERVED) {
                Inventory inventory = inventoryRepository
                        .findByProductIdAndWarehouseId(reservation.getProductId(), reservation.getWarehouseId())
                        .orElseThrow(() -> new ResourceNotFoundException("Inventory",
                                "productId + warehouseId",
                                reservation.getProductId() + ", " + reservation.getWarehouseId()));

                inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
                inventoryRepository.save(inventory);
            }

            reservation.setStatus(ReservationStatus.CANCELLED);
            reservationRepository.save(reservation);
        }

        log.info("Reservations cancelled for orderId={}", orderId);
        return toReservationResponse(reservations.get(0));
    }

    @Scheduled(fixedRateString = "${inventory.reservation.expiry-check-interval-ms:60000}")
    public void expireStaleReservations() {
        Instant now = Instant.now();
        List<StockReservation> staleReservations = reservationRepository
                .findByStatusAndReservedUntilBefore(ReservationStatus.RESERVED, now);

        if (staleReservations.isEmpty()) {
            return;
        }

        for (StockReservation reservation : staleReservations) {
            reservation.setStatus(ReservationStatus.EXPIRED);

            // Release reserved quantity back to available
            Inventory inventory = inventoryRepository
                    .findByProductIdAndWarehouseId(reservation.getProductId(), reservation.getWarehouseId())
                    .orElse(null);

            if (inventory != null) {
                inventory.setReservedQuantity(
                        Math.max(0, inventory.getReservedQuantity() - reservation.getQuantity()));
                inventoryRepository.save(inventory);
            }

            reservationRepository.save(reservation);
        }

        log.info("Expired {} stale stock reservations", staleReservations.size());
    }

    // --- Mappers ---

    private InventoryResponse toResponse(Inventory i) {
        return new InventoryResponse(
                i.getId(),
                i.getProductId(),
                i.getWarehouseId(),
                i.getQuantity(),
                i.getReservedQuantity(),
                i.getAvailableQuantity(),
                i.getReorderLevel(),
                i.getReorderQuantity(),
                i.getLastRestockDate(),
                i.getCreatedAt(),
                i.getUpdatedAt()
        );
    }

    private StockReservationResponse toReservationResponse(StockReservation r) {
        return new StockReservationResponse(
                r.getId(),
                r.getProductId(),
                r.getWarehouseId(),
                r.getOrderId(),
                r.getQuantity(),
                r.getStatus(),
                r.getReservedUntil(),
                r.getCreatedAt()
        );
    }
}
