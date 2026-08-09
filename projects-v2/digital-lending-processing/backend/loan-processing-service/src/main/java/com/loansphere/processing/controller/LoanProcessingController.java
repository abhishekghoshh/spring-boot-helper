package com.loansphere.processing.controller;

import com.loansphere.common.api.ApiResponse;
import com.loansphere.common.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/processing")
@RequiredArgsConstructor
public class LoanProcessingController {

    @GetMapping("/reviews")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN', 'LOAN_MANAGER', 'UNDERWRITER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPendingReviews() {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("pending", 0);
        dashboard.put("inProgress", 0);
        dashboard.put("completedToday", 0);
        dashboard.put("totalQueue", 0);
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    @PostMapping("/reviews/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN', 'LOAN_MANAGER', 'UNDERWRITER')")
    public ResponseEntity<ApiResponse<Map<String, String>>> approve(
            @PathVariable String id,
            @CurrentUser String reviewerId,
            @RequestBody ApprovalRequest request) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("applicationId", id);
        result.put("status", "APPROVED");
        result.put("reviewerId", reviewerId);
        result.put("message", "Loan application approved successfully");
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/reviews/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN', 'LOAN_MANAGER', 'UNDERWRITER')")
    public ResponseEntity<ApiResponse<Map<String, String>>> reject(
            @PathVariable String id,
            @CurrentUser String reviewerId,
            @RequestBody RejectionRequest request) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("applicationId", id);
        result.put("status", "REJECTED");
        result.put("reviewerId", reviewerId);
        result.put("reason", request.reason());
        result.put("message", "Loan application rejected");
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/reviews/{id}/request-info")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN', 'LOAN_MANAGER', 'UNDERWRITER')")
    public ResponseEntity<ApiResponse<Map<String, String>>> requestInfo(
            @PathVariable String id, @RequestBody InfoRequest request) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("applicationId", id);
        result.put("status", "NEEDS_INFO");
        result.put("message", request.message());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    public record ApprovalRequest(String notes) {}
    public record RejectionRequest(String reason) {}
    public record InfoRequest(String message) {}
}
