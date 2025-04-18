package io.github.abhishekghoshh.products.repository.impl;

import io.github.abhishekghoshh.products.entity.Product;
import io.github.abhishekghoshh.products.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Qualifier("ProductHibernateRepository")
public class ProductHibernateRepository implements ProductRepository {
    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public List<Product> findAll() {
        return entityManager.createQuery("SELECT p FROM Product p", Product.class)
                .getResultList();
    }

    @Override
    public Page<Product> findAll(PageRequest pageRequest) {
        return null;
    }

    @Override
    public Optional<Product> findById(Long id) {
        Product product = entityManager.find(Product.class, id);
        return Optional.ofNullable(product);
    }

    @Override
    @Transactional
    public Product save(Product product) {
        entityManager.persist(product);
        return product;
    }

    @Override
    @Transactional
    public Product update(Product product) {
        if (entityManager.find(Product.class, product.getId()) == null) {
            throw new IllegalArgumentException("Product with ID " + product.getId() + " does not exist");
        }
        return entityManager.merge(product);
    }

    @Override
    @Transactional
    public void delete(Product product) {
        entityManager.remove(product);
    }
}
