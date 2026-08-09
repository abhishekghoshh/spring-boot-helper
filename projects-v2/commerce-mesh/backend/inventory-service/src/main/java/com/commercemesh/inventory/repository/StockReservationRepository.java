package com.commercemesh.inventory.repository;

import com.commercemesh.inventory.entity.ReservationStatus;
import com.commercemesh.inventory.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    List<StockReservation> findByOrderId(Long orderId);

    Optional<StockReservation> findByOrderIdAndProductIdAndWarehouseId(Long orderId, Long productId, Long warehouseId);

    List<StockReservation> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    List<StockReservation> findByStatusAndReservedUntilBefore(ReservationStatus status, Instant timestamp);

    Optional<StockReservation> findByOrderIdAndStatus(Long orderId, ReservationStatus status);
}
