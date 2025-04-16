package io.github.abhishekghoshh.products.repository.impl;

import io.github.abhishekghoshh.products.entity.Product;
import io.github.abhishekghoshh.products.repository.ProductDataJpaRepository;
import io.github.abhishekghoshh.products.repository.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Qualifier("ProductDataJpaRepositoryWrapper")
@AllArgsConstructor
public class ProductDataJpaRepositoryWrapper implements ProductRepository {

    private final ProductDataJpaRepository productDataJpaRepository;

    @Override
    public List<Product> findAll() {
        return productDataJpaRepository.findAll();
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productDataJpaRepository.findById(id);
    }

    @Override
    public Product save(Product product) {
        return productDataJpaRepository.save(product);
    }

    @Override
    public Product update(Product product) {
        if (!productDataJpaRepository.existsById(product.getId())) {
            throw new IllegalArgumentException("Product with ID " + product.getId() + " does not exist");
        }
        return save(product);
    }

    @Override
    public void delete(Product product) {
        productDataJpaRepository.delete(product);
    }
}
