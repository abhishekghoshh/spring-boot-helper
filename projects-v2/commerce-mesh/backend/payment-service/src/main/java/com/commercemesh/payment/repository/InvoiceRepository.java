package com.commercemesh.payment.repository;

import com.commercemesh.payment.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    Optional<Invoice> findByOrderId(String orderId);

    Page<Invoice> findByUserId(String userId, Pageable pageable);
}
