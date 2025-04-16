package io.github.abhishekghoshh.products.service.impl;

import io.github.abhishekghoshh.products.dto.ProductDTO;
import io.github.abhishekghoshh.products.entity.Product;
import io.github.abhishekghoshh.products.exception.ApiException;
import io.github.abhishekghoshh.products.repository.ProductRepository;
import io.github.abhishekghoshh.products.service.ProductService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {


    private final ProductRepository repository;

    public ProductServiceImpl(
            @Qualifier("ProductJDBCRepository") ProductRepository repository
    ) {
        this.repository = repository;
    }


    public List<ProductDTO> getAll() {
        return repository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ProductDTO> getById(Long id) throws ApiException {
        Product product = repository.findById(id)
                .orElseThrow(() -> new ApiException("Product not found", HttpStatus.NOT_FOUND.value()));
        return Optional.of(convertToDTO(product));
    }

    @Override
    public ProductDTO save(ProductDTO productDTO) {
        Product product = convertToEntity(productDTO);
        Product savedProduct = repository.save(product);
        return convertToDTO(savedProduct);
    }

    @Override
    public ProductDTO updateById(Long id, ProductDTO productDTO) throws Exception {
        Product product = repository.findById(id)
                .orElseThrow(() -> new ApiException("Product not found", HttpStatus.NOT_FOUND.value()));
        product.setName(productDTO.name());
        product.setDescription(productDTO.description());
        product.setPrice(productDTO.price());
        Product updatedProduct = repository.update(product);
        return convertToDTO(updatedProduct);
    }

    @Override
    public void deleteById(Long id) throws ApiException {
        Product product = repository.findById(id)
                .orElseThrow(() -> new ApiException("Product not found", HttpStatus.NOT_FOUND.value()));
        repository.delete(product);
    }
}
