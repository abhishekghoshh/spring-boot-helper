package com.commercesphere.catalog.mapper;

import com.commercesphere.catalog.document.Category;
import com.commercesphere.catalog.dto.CategoryDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryDto toDto(Category category);
    Category toEntity(CategoryDto dto);
}
