package com.loansphere.offer.controller;

import com.loansphere.common.api.ApiResponse;
import com.loansphere.offer.model.LoanOffer;
import com.loansphere.offer.service.LoanOfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/offers")
@RequiredArgsConstructor
public class LoanOfferController {

    private final LoanOfferService offerService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LoanOffer>>> getActiveOffers() {
        return ResponseEntity.ok(ApiResponse.success(offerService.getActiveOffers()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LoanOffer>> getOffer(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(offerService.getOffer(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN', 'LOAN_MANAGER')")
    public ResponseEntity<ApiResponse<LoanOffer>> createOffer(@Valid @RequestBody LoanOffer offer) {
        return ResponseEntity.ok(ApiResponse.success(offerService.create(offer)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN', 'LOAN_MANAGER')")
    public ResponseEntity<ApiResponse<LoanOffer>> updateOffer(@PathVariable String id, @Valid @RequestBody LoanOffer offer) {
        return ResponseEntity.ok(ApiResponse.success(offerService.update(id, offer)));
    }

    @GetMapping("/{id}/emi")
    public ResponseEntity<ApiResponse<Double>> calculateEmi(
            @PathVariable String id,
            @RequestParam double amount,
            @RequestParam int tenureMonths) {
        LoanOffer offer = offerService.getOffer(id);
        double emi = offerService.calculateMonthlyEmi(amount, offer.getInterestRate(), tenureMonths);
        return ResponseEntity.ok(ApiResponse.success(emi));
    }
}
