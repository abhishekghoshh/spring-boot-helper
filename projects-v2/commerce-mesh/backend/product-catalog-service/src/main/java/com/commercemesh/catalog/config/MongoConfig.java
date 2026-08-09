package com.commercemesh.catalog.config;

import com.commercemesh.catalog.document.Category;
import com.commercemesh.catalog.document.Product;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import jakarta.annotation.PostConstruct;
import org.bson.Document;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;

@Configuration
@EnableMongoAuditing
public class MongoConfig {

    private final MongoTemplate mongoTemplate;

    public MongoConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void initIndexes() {
        // Ensure annotated indexes from entity classes are created
        IndexOperations productIndexOps = mongoTemplate.indexOps(Product.class);
        IndexResolver productResolver = new MongoPersistentEntityIndexResolver(
                mongoTemplate.getConverter().getMappingContext());
        productResolver.resolveIndexFor(Product.class).forEach(productIndexOps::ensureIndex);

        IndexOperations categoryIndexOps = mongoTemplate.indexOps(Category.class);
        IndexResolver categoryResolver = new MongoPersistentEntityIndexResolver(
                mongoTemplate.getConverter().getMappingContext());
        categoryResolver.resolveIndexFor(Category.class).forEach(categoryIndexOps::ensureIndex);

        // Create compound text index on Product name and description
        MongoDatabaseFactory factory = mongoTemplate.getMongoDatabaseFactory();
        MongoDatabase database = factory.getMongoDatabase();
        MongoCollection<Document> productsCollection = database.getCollection("products");

        Document textIndexKeys = new Document()
                .append("name", "text")
                .append("description", "text");
        IndexOptions textIndexOptions = new IndexOptions()
                .name("product_text_search")
                .background(true);
        productsCollection.createIndex(textIndexKeys, textIndexOptions);

        // Create text index on Category name
        MongoCollection<Document> categoriesCollection = database.getCollection("categories");
        Document categoryTextIndexKeys = new Document("name", "text");
        IndexOptions categoryTextIndexOptions = new IndexOptions()
                .name("category_text_search")
                .background(true);
        categoriesCollection.createIndex(categoryTextIndexKeys, categoryTextIndexOptions);
    }
}
