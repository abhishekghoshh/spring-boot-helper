package io.github.abhishekghoshh.products.repository;

import io.github.abhishekghoshh.products.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

public interface ProductRepository  {
    List<Product> findAll();

    Page<Product> findAll(PageRequest pageRequest);

    Optional<Product> findById(Long id);

    Product save(Product product);

    Product update(Product product) throws Exception;

    void delete(Product product);

}
