package com.commercesphere.catalog.repository;

import com.commercesphere.catalog.document.Brand;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandRepository extends MongoRepository<Brand, String> {}
