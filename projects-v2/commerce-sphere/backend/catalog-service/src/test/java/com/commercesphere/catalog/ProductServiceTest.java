package com.commercesphere.catalog;

import com.commercesphere.catalog.document.Product;
import com.commercesphere.catalog.dto.ProductCreateRequest;
import com.commercesphere.catalog.dto.ProductDto;
import com.commercesphere.catalog.mapper.ProductMapperImpl;
import com.commercesphere.catalog.repository.BrandRepository;
import com.commercesphere.catalog.repository.CategoryRepository;
import com.commercesphere.catalog.repository.ProductRepository;
import com.commercesphere.catalog.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private BrandRepository brandRepository;

    @Test
    void getById_shouldReturnProduct() {
        ProductService service = new ProductService(productRepository, categoryRepository, brandRepository, new ProductMapperImpl());
        Product p = Product.builder().id("p1").name("Test").price(BigDecimal.TEN).build();
        when(productRepository.findById("p1")).thenReturn(Optional.of(p));

        ProductDto result = service.getById("p1");
        assertEquals("Test", result.name());
    }
}
