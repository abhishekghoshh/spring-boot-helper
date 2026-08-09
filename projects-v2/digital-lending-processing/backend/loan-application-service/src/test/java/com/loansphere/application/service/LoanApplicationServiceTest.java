package com.loansphere.application.service;

import com.loansphere.application.model.LoanApplication;
import com.loansphere.application.repository.LoanApplicationRepository;
import com.loansphere.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @InjectMocks
    private LoanApplicationService loanApplicationService;

    @Test
    void shouldSubmitApplication() {
        LoanApplication app = new LoanApplication();
        app.setUserId("user1"); app.setOfferId("offer1");
        app.setAmount(500000.0); app.setTenureMonths(36);
        app.setStatus("DRAFT");

        when(loanApplicationRepository.save(any())).thenReturn(app);

        LoanApplication result = loanApplicationService.submitApplication(app);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("DRAFT");
    }

    @Test
    void shouldFindById() {
        LoanApplication app = new LoanApplication();
        app.setId("app1"); app.setUserId("user1");

        when(loanApplicationRepository.findById("app1")).thenReturn(Optional.of(app));

        LoanApplication result = loanApplicationService.findById("app1");

        assertThat(result.getUserId()).isEqualTo("user1");
    }

    @Test
    void shouldThrowWhenApplicationNotFound() {
        when(loanApplicationRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanApplicationService.findById("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldGetApplicationsByUserId() {
        LoanApplication app = new LoanApplication();
        app.setUserId("user1");

        when(loanApplicationRepository.findByUserId("user1")).thenReturn(List.of(app));

        List<LoanApplication> apps = loanApplicationService.findByUserId("user1");

        assertThat(apps).hasSize(1);
    }

    @Test
    void shouldUpdateApplicationStatus() {
        LoanApplication app = new LoanApplication();
        app.setId("app1"); app.setStatus("SUBMITTED");

        when(loanApplicationRepository.findById("app1")).thenReturn(Optional.of(app));
        when(loanApplicationRepository.save(any())).thenReturn(app);

        LoanApplication result = loanApplicationService.updateStatus("app1", "UNDER_REVIEW");

        assertThat(result.getStatus()).isEqualTo("UNDER_REVIEW");
        verify(loanApplicationRepository).save(app);
    }
}
