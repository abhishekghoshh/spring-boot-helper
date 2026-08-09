package com.loansphere.application.controller;

import com.loansphere.common.api.ApiResponse;
import com.loansphere.common.security.CurrentUser;
import com.loansphere.application.model.LoanApplication;
import com.loansphere.application.service.LoanApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class LoanApplicationController {

    private final LoanApplicationService applicationService;

    @PostMapping
    public ResponseEntity<ApiResponse<LoanApplication>> submit(
            @CurrentUser String userId,
            @Valid @RequestBody SubmitRequest request) {
        LoanApplication app = applicationService.submit(
                userId, request.offerId(), request.offerName(),
                request.loanAmount(), request.tenureMonths(), request.purpose());
        return ResponseEntity.ok(ApiResponse.success("Application submitted", app));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LoanApplication>>> getMyApplications(@CurrentUser String userId) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.getByUserId(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LoanApplication>> getApplication(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.getById(id)));
    }

    public record SubmitRequest(
            String offerId,
            String offerName,
            @Min(1000) double loanAmount,
            @Min(3) int tenureMonths,
            String purpose) {}
}
