package com.loansphere.customer.repository;

import com.loansphere.customer.model.CustomerProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface CustomerProfileRepository extends MongoRepository<CustomerProfile, String> {
    Optional<CustomerProfile> findByUserId(String userId);
}
