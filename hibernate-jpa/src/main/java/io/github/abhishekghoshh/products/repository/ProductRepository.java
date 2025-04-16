package io.github.abhishekghoshh.products.repository;

import io.github.abhishekghoshh.products.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository  {
    List<Product> findAll();

    Optional<Product> findById(Long id);

    Product save(Product product);

    Product update(Product product) throws Exception;

    void delete(Product product);

}
