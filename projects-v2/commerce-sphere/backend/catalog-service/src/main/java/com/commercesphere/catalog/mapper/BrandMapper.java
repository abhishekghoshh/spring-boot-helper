package com.commercesphere.catalog.mapper;

import com.commercesphere.catalog.document.Brand;
import com.commercesphere.catalog.dto.BrandDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BrandMapper {
    BrandDto toDto(Brand brand);
    Brand toEntity(BrandDto dto);
}
