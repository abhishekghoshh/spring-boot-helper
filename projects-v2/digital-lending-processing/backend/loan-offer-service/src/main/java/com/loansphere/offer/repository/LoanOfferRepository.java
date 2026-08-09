package com.loansphere.offer.repository;

import com.loansphere.offer.model.LoanOffer;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.Instant;
import java.util.List;

public interface LoanOfferRepository extends MongoRepository<LoanOffer, String> {
    List<LoanOffer> findByActiveTrue();
    List<LoanOffer> findByActiveTrueAndExpiresAtAfter(Instant now);
}
