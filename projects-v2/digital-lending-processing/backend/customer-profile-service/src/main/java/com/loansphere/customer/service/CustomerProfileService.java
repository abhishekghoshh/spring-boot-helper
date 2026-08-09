package com.loansphere.customer.service;

import com.loansphere.common.exception.ResourceNotFoundException;
import com.loansphere.customer.model.CustomerProfile;
import com.loansphere.customer.repository.CustomerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerProfileService {

    private final CustomerProfileRepository profileRepository;

    public CustomerProfile create(CustomerProfile profile) {
        return profileRepository.save(profile);
    }

    public CustomerProfile getByUserId(String userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomerProfile", "userId", userId));
    }

    public CustomerProfile update(String userId, CustomerProfile updated) {
        CustomerProfile existing = getByUserId(userId);
        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        existing.setPhone(updated.getPhone());
        existing.setDateOfBirth(updated.getDateOfBirth());
        existing.setGender(updated.getGender());
        existing.setAnnualIncome(updated.getAnnualIncome());
        existing.setEmploymentType(updated.getEmploymentType());
        existing.setEmployerName(updated.getEmployerName());
        existing.setCreditScore(updated.getCreditScore());
        return profileRepository.save(existing);
    }
}
