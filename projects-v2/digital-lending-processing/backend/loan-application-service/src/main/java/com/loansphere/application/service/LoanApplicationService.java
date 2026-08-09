package com.loansphere.application.service;

import com.loansphere.application.model.LoanApplication;
import com.loansphere.application.repository.LoanApplicationRepository;
import com.loansphere.common.exception.BadRequestException;
import com.loansphere.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanApplicationService {

    private final LoanApplicationRepository applicationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public LoanApplication submit(String userId, String offerId, String offerName,
                                   double loanAmount, int tenureMonths, String purpose) {
        LoanApplication application = new LoanApplication();
        application.setUserId(userId);
        application.setOfferId(offerId);
        application.setOfferName(offerName);
        application.setLoanAmount(loanAmount);
        application.setTenureMonths(tenureMonths);
        application.setPurpose(purpose);
        application.setStatus("SUBMITTED");

        LoanApplication saved = applicationRepository.save(application);

        eventPublisher.publishEvent(new LoanSubmittedEvent(
                saved.getId(), userId, offerId, loanAmount, tenureMonths));

        return saved;
    }

    public List<LoanApplication> getByUserId(String userId) {
        return applicationRepository.findByUserId(userId);
    }

    public LoanApplication getById(String id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LoanApplication", "id", id));
    }

    public LoanApplication updateStatus(String id, String status, String notes) {
        LoanApplication app = getById(id);
        app.setStatus(status);
        app.setReviewerNotes(notes);

        if ("APPROVED".equals(status)) {
            eventPublisher.publishEvent(new LoanApprovedEvent(app.getId(), app.getUserId(), app.getLoanAmount()));
        } else if ("REJECTED".equals(status)) {
            app.setRejectionReason(notes);
            eventPublisher.publishEvent(new LoanRejectedEvent(app.getId(), app.getUserId(), notes));
        }

        return applicationRepository.save(app);
    }

    public List<LoanApplication> getByStatus(String status) {
        return applicationRepository.findByStatus(status);
    }

    public long countByStatus(String status) {
        return applicationRepository.countByStatus(status);
    }

    // Events
    public record LoanSubmittedEvent(String applicationId, String userId, String offerId, double amount, int tenure) {}
    public record LoanApprovedEvent(String applicationId, String userId, double amount) {}
    public record LoanRejectedEvent(String applicationId, String userId, String reason) {}
}
