package com.commercemesh.user.service;

import com.commercemesh.user.dto.AddressRequest;
import com.commercemesh.user.dto.AddressResponse;
import com.commercemesh.user.entity.Address;
import com.commercemesh.user.exception.ResourceNotFoundException;
import com.commercemesh.user.repository.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(Long userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(AddressResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public AddressResponse addAddress(Long userId, AddressRequest request) {
        // If this is set as default, unset all other defaults for this user
        if (request.isDefault()) {
            addressRepository.findByUserIdAndIsDefaultTrue(userId)
                    .ifPresent(addr -> {
                        addr.setDefault(false);
                        addressRepository.save(addr);
                    });
        }

        Address address = new Address();
        address.setUserId(userId);
        address.setAddressLine1(request.addressLine1());
        address.setAddressLine2(request.addressLine2());
        address.setCity(request.city());
        address.setState(request.state());
        address.setCountry(request.country());
        address.setZipCode(request.zipCode());
        address.setDefault(request.isDefault());
        address.setAddressType(request.toAddressType());

        Address saved = addressRepository.save(address);
        return AddressResponse.fromEntity(saved);
    }

    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));

        if (request.isDefault() && !address.isDefault()) {
            addressRepository.findByUserIdAndIsDefaultTrue(userId)
                    .ifPresent(addr -> {
                        addr.setDefault(false);
                        addressRepository.save(addr);
                    });
        }

        address.setAddressLine1(request.addressLine1());
        address.setAddressLine2(request.addressLine2());
        address.setCity(request.city());
        address.setState(request.state());
        address.setCountry(request.country());
        address.setZipCode(request.zipCode());
        address.setDefault(request.isDefault());
        address.setAddressType(request.toAddressType());

        Address saved = addressRepository.save(address);
        return AddressResponse.fromEntity(saved);
    }

    public void deleteAddress(Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
        addressRepository.delete(address);
    }

    public AddressResponse setDefaultAddress(Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));

        // Unset all defaults for this user
        addressRepository.findByUserId(userId).forEach(addr -> {
            if (addr.isDefault()) {
                addr.setDefault(false);
                addressRepository.save(addr);
            }
        });

        address.setDefault(true);
        Address saved = addressRepository.save(address);
        return AddressResponse.fromEntity(saved);
    }
}
