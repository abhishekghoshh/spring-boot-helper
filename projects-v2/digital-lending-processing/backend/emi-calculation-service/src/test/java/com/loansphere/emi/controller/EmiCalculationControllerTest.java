package com.loansphere.emi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmiCalculationController.class)
class EmiCalculationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCalculateEmi() throws Exception {
        mockMvc.perform(get("/api/v1/emi/calculate")
                        .param("principal", "100000")
                        .param("annualInterestRate", "10")
                        .param("tenureMonths", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.monthlyEmi").isNumber())
                .andExpect(jsonPath("$.data.principal").value(100000.0))
                .andExpect(jsonPath("$.data.totalInterest").isNumber());
    }

    @Test
    void shouldRejectNegativePrincipal() throws Exception {
        mockMvc.perform(get("/api/v1/emi/calculate")
                        .param("principal", "-1000")
                        .param("annualInterestRate", "10")
                        .param("tenureMonths", "12"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectZeroTenure() throws Exception {
        mockMvc.perform(get("/api/v1/emi/calculate")
                        .param("principal", "100000")
                        .param("annualInterestRate", "10")
                        .param("tenureMonths", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGenerateAmortizationSchedule() throws Exception {
        mockMvc.perform(get("/api/v1/emi/amortization-schedule")
                        .param("principal", "100000")
                        .param("annualInterestRate", "10")
                        .param("tenureMonths", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(6));
    }

    @Test
    void shouldCalculatePrepaymentImpact() throws Exception {
        mockMvc.perform(get("/api/v1/emi/prepayment")
                        .param("principal", "100000")
                        .param("annualInterestRate", "10")
                        .param("tenureMonths", "12")
                        .param("prepaymentAmount", "20000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.interestSaved").isNumber());
    }

    @Test
    void shouldHandleZeroInterestRate() throws Exception {
        mockMvc.perform(get("/api/v1/emi/calculate")
                        .param("principal", "100000")
                        .param("annualInterestRate", "0")
                        .param("tenureMonths", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
