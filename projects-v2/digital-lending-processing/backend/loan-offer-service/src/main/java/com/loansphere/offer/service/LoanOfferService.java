package com.loansphere.offer.service;

import com.loansphere.common.exception.ResourceNotFoundException;
import com.loansphere.offer.model.LoanOffer;
import com.loansphere.offer.repository.LoanOfferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanOfferService {

    private final LoanOfferRepository offerRepository;

    @Cacheable(value = "activeOffers", key = "'all'")
    public List<LoanOffer> getActiveOffers() {
        return offerRepository.findByActiveTrueAndExpiresAtAfter(Instant.now());
    }

    public LoanOffer getOffer(String id) {
        return offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LoanOffer", "id", id));
    }

    @CacheEvict(value = "activeOffers", key = "'all'")
    public LoanOffer create(LoanOffer offer) {
        return offerRepository.save(offer);
    }

    @CacheEvict(value = "activeOffers", key = "'all'")
    public LoanOffer update(String id, LoanOffer updated) {
        LoanOffer existing = getOffer(id);
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setMinAmount(updated.getMinAmount());
        existing.setMaxAmount(updated.getMaxAmount());
        existing.setInterestRate(updated.getInterestRate());
        existing.setMinTenureMonths(updated.getMinTenureMonths());
        existing.setMaxTenureMonths(updated.getMaxTenureMonths());
        existing.setProcessingFee(updated.getProcessingFee());
        existing.setMinCreditScore(updated.getMinCreditScore());
        existing.setMinAnnualIncome(updated.getMinAnnualIncome());
        existing.setActive(updated.isActive());
        return offerRepository.save(existing);
    }

    public double calculateMonthlyEmi(double principal, double annualRate, int tenureMonths) {
        double monthlyRate = annualRate / 12 / 100;
        return principal * monthlyRate * Math.pow(1 + monthlyRate, tenureMonths)
                / (Math.pow(1 + monthlyRate, tenureMonths) - 1);
    }
}
