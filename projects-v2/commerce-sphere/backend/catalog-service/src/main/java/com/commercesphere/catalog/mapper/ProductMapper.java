package com.commercesphere.catalog.mapper;

import com.commercesphere.catalog.document.Product;
import com.commercesphere.catalog.dto.ProductCreateRequest;
import com.commercesphere.catalog.dto.ProductDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    ProductDto toDto(Product product);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categoryName", ignore = true)
    @Mapping(target = "brandName", ignore = true)
    @Mapping(target = "imageUrls", ignore = true)
    @Mapping(target = "specifications", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toEntity(ProductCreateRequest request);
}
