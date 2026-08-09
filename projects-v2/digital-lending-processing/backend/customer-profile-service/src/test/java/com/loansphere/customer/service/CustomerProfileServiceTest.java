package com.loansphere.customer.service;

import com.loansphere.customer.model.CustomerProfile;
import com.loansphere.customer.repository.CustomerProfileRepository;
import com.loansphere.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceTest {

    @Mock
    private CustomerProfileRepository repository;

    @InjectMocks
    private CustomerProfileService customerProfileService;

    @Test
    void shouldCreateProfile() {
        CustomerProfile profile = new CustomerProfile();
        profile.setUserId("user1");
        profile.setFirstName("John");

        when(repository.save(any())).thenReturn(profile);

        CustomerProfile result = customerProfileService.createProfile("user1", "John", "Doe",
                "john@test.com", "+1234567890");

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo("user1");
    }

    @Test
    void shouldFindProfileByUserId() {
        CustomerProfile profile = new CustomerProfile();
        profile.setUserId("user1");

        when(repository.findByUserId("user1")).thenReturn(Optional.of(profile));

        CustomerProfile result = customerProfileService.findByUserId("user1");

        assertThat(result.getUserId()).isEqualTo("user1");
    }

    @Test
    void shouldThrowWhenProfileNotFound() {
        when(repository.findByUserId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerProfileService.findByUserId("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
