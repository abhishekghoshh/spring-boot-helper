package com.loansphere.emi.controller;

import com.loansphere.common.api.ApiResponse;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/emi")
public class EmiCalculationController {

    @GetMapping("/calculate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculateEmi(
            @RequestParam @Min(1000) double principal,
            @RequestParam @Min(0) double annualInterestRate,
            @RequestParam @Min(1) int tenureMonths) {

        double monthlyRate = annualInterestRate / 12 / 100;
        double emi = principal * monthlyRate * Math.pow(1 + monthlyRate, tenureMonths)
                / (Math.pow(1 + monthlyRate, tenureMonths) - 1);
        double totalPayment = emi * tenureMonths;
        double totalInterest = totalPayment - principal;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("monthlyEmi", Math.round(emi * 100.0) / 100.0);
        result.put("principal", principal);
        result.put("totalInterest", Math.round(totalInterest * 100.0) / 100.0);
        result.put("totalPayment", Math.round(totalPayment * 100.0) / 100.0);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/amortization-schedule")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> amortizationSchedule(
            @RequestParam @Min(1000) double principal,
            @RequestParam @Min(0) double annualInterestRate,
            @RequestParam @Min(1) int tenureMonths) {

        double monthlyRate = annualInterestRate / 12 / 100;
        double emi = principal * monthlyRate * Math.pow(1 + monthlyRate, tenureMonths)
                / (Math.pow(1 + monthlyRate, tenureMonths) - 1);

        double remainingPrincipal = principal;
        List<Map<String, Object>> schedule = new ArrayList<>();

        for (int month = 1; month <= tenureMonths; month++) {
            double interestComponent = remainingPrincipal * monthlyRate;
            double principalComponent = emi - interestComponent;
            remainingPrincipal -= principalComponent;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", month);
            row.put("emi", Math.round(emi * 100.0) / 100.0);
            row.put("principal", Math.round(principalComponent * 100.0) / 100.0);
            row.put("interest", Math.round(interestComponent * 100.0) / 100.0);
            row.put("remainingPrincipal", Math.round(Math.max(0, remainingPrincipal) * 100.0) / 100.0);
            schedule.add(row);
        }

        return ResponseEntity.ok(ApiResponse.success(schedule));
    }

    @GetMapping("/prepayment")
    public ResponseEntity<ApiResponse<Map<String, Object>>> prepayment(
            @RequestParam @Min(1000) double principal,
            @RequestParam @Min(0) double annualInterestRate,
            @RequestParam @Min(1) int tenureMonths,
            @RequestParam @Min(0) double prepaymentAmount) {

        double monthlyRate = annualInterestRate / 12 / 100;
        double emi = principal * monthlyRate * Math.pow(1 + monthlyRate, tenureMonths)
                / (Math.pow(1 + monthlyRate, tenureMonths) - 1);

        double remainingPrincipal = principal;
        int monthsPaid = 0;

        for (int month = 1; month <= tenureMonths; month++) {
            double interestComponent = remainingPrincipal * monthlyRate;
            double principalComponent = emi - interestComponent;
            remainingPrincipal -= principalComponent;
            monthsPaid++;

            if (remainingPrincipal <= 0) break;
        }

        double newPrincipal = remainingPrincipal - prepaymentAmount;
        double newEmi = newPrincipal > 0 ? newPrincipal * monthlyRate * Math.pow(1 + monthlyRate, tenureMonths - monthsPaid)
                / (Math.pow(1 + monthlyRate, tenureMonths - monthsPaid) - 1) : 0;

        double interestSaved = (emi * tenureMonths - principal) - (emi * monthsPaid + newEmi * (tenureMonths - monthsPaid) - principal);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("originalMonthlyEmi", Math.round(emi * 100.0) / 100.0);
        result.put("newMonthlyEmi", Math.round(newEmi * 100.0) / 100.0);
        result.put("monthsCompleted", monthsPaid);
        result.put("remainingMonths", tenureMonths - monthsPaid);
        result.put("interestSaved", Math.round(Math.max(0, interestSaved) * 100.0) / 100.0);

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
