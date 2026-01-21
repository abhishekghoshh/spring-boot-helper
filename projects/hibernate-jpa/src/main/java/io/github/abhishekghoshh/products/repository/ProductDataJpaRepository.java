package io.github.abhishekghoshh.products.repository;

import io.github.abhishekghoshh.products.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductDataJpaRepository extends JpaRepository<Product, Long> {
}