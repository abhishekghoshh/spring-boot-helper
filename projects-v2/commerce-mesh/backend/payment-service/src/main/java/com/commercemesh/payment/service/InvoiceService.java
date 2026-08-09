package com.commercemesh.payment.service;

import com.commercemesh.payment.dto.InvoiceResponse;
import com.commercemesh.payment.dto.PageResponse;
import com.commercemesh.payment.entity.Invoice;
import com.commercemesh.payment.exception.ResourceNotFoundException;
import com.commercemesh.payment.repository.InvoiceRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(String id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
        return mapToInvoiceResponse(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByOrderId(String orderId) {
        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found for order: " + orderId));
        return mapToInvoiceResponse(invoice);
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> getUserInvoices(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "generatedAt"));
        Page<Invoice> invoicePage = invoiceRepository.findByUserId(userId, pageable);

        List<InvoiceResponse> responses = invoicePage.getContent().stream()
                .map(this::mapToInvoiceResponse)
                .toList();

        return new PageResponse<>(
                responses,
                invoicePage.getNumber(),
                invoicePage.getSize(),
                invoicePage.getTotalElements(),
                invoicePage.getTotalPages()
        );
    }

    private InvoiceResponse mapToInvoiceResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getOrderId(),
                invoice.getOrderNumber(),
                invoice.getUserId(),
                invoice.getAmount(),
                invoice.getCurrency(),
                invoice.getStatus(),
                invoice.getItems(),
                invoice.getCreatedAt(),
                invoice.getGeneratedAt()
        );
    }
}
