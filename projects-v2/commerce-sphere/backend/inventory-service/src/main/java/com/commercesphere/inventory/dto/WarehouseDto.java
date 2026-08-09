package com.commercesphere.inventory.dto;
import jakarta.validation.constraints.NotBlank;
public record WarehouseDto(String id, @NotBlank String name, @NotBlank String code, String street, String city, String state, String zipCode, String country, boolean active) {}
