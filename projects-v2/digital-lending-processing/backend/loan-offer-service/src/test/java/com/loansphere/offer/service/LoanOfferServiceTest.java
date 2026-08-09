package com.loansphere.offer.service;

import com.loansphere.offer.model.LoanOffer;
import com.loansphere.offer.repository.LoanOfferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanOfferServiceTest {

    @Mock
    private LoanOfferRepository loanOfferRepository;

    @InjectMocks
    private LoanOfferService loanOfferService;

    @Test
    void shouldGetAllOffers() {
        LoanOffer offer = new LoanOffer();
        offer.setName("Home Loan");
        offer.setInterestRate(8.5);

        when(loanOfferRepository.findAll()).thenReturn(List.of(offer));

        List<LoanOffer> offers = loanOfferService.getAllOffers();

        assertThat(offers).hasSize(1);
        assertThat(offers.get(0).getName()).isEqualTo("Home Loan");
    }

    @Test
    void shouldCreateOffer() {
        LoanOffer offer = new LoanOffer();
        offer.setName("Car Loan");
        offer.setInterestRate(9.0);

        when(loanOfferRepository.save(any())).thenReturn(offer);

        LoanOffer result = loanOfferService.createOffer(offer);

        assertThat(result.getName()).isEqualTo("Car Loan");
    }
}
