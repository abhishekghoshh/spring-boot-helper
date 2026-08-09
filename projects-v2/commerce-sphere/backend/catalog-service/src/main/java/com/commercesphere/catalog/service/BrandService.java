package com.commercesphere.catalog.service;

import com.commercesphere.catalog.document.Brand;
import com.commercesphere.catalog.dto.BrandDto;
import com.commercesphere.catalog.mapper.BrandMapper;
import com.commercesphere.catalog.repository.BrandRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    public BrandService(BrandRepository brandRepository, BrandMapper brandMapper) {
        this.brandRepository = brandRepository;
        this.brandMapper = brandMapper;
    }

    @Cacheable(value = "brands")
    public List<BrandDto> getAll() {
        return brandRepository.findAll().stream().map(brandMapper::toDto).toList();
    }

    @Cacheable(value = "brands", key = "#id")
    public BrandDto getById(String id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found"));
        return brandMapper.toDto(brand);
    }

    @CacheEvict(value = "brands", allEntries = true)
    public BrandDto create(BrandDto dto) {
        Brand brand = brandMapper.toEntity(dto);
        return brandMapper.toDto(brandRepository.save(brand));
    }

    @CacheEvict(value = "brands", allEntries = true)
    public BrandDto update(String id, BrandDto dto) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found"));
        brand.setName(dto.name());
        brand.setDescription(dto.description());
        brand.setLogoUrl(dto.logoUrl());
        return brandMapper.toDto(brandRepository.save(brand));
    }

    @CacheEvict(value = "brands", allEntries = true)
    public void delete(String id) {
        brandRepository.deleteById(id);
    }
}
