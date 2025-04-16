package io.github.abhishekghoshh.products.service;

import io.github.abhishekghoshh.products.dto.ProductDTO;
import io.github.abhishekghoshh.products.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductService {
    List<ProductDTO> getAll();

    Optional<ProductDTO> getById(Long id) throws Exception;

    ProductDTO save(ProductDTO productDTO);

    ProductDTO updateById(Long id, ProductDTO productDTO) throws Exception;

    void deleteById(Long id) throws Exception;

    // Convert Product Entity to ProductDTO
    default ProductDTO convertToDTO(Product product) {
        return new ProductDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice()
        );
    }

    // Convert ProductDTO to Product Entity
    default Product convertToEntity(ProductDTO productDTO) {
        Product product = new Product();
        product.setName(productDTO.name());
        product.setDescription(productDTO.description());
        product.setPrice(productDTO.price());
        return product;
    }
}
