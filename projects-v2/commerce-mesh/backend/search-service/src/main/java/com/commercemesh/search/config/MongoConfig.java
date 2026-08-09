package com.commercemesh.search.config;

import com.commercemesh.search.document.ProductDocument;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import jakarta.annotation.PostConstruct;

@Configuration
public class MongoConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);

    private final MongoTemplate mongoTemplate;

    public MongoConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Ensure text indexes on name, description, brand, and tags at startup.
     * Spring Data MongoDB's @TextIndexed annotations handle index creation during
     * initial collection setup; this is a safety net for pre-existing collections.
     */
    @PostConstruct
    public void ensureTextIndexes() {
        try {
            var indexOps = mongoTemplate.indexOps(ProductDocument.class);
            var existingIndexes = indexOps.getIndexInfo();

            boolean textIndexExists = existingIndexes.stream()
                    .anyMatch(idx -> idx.getIndexFields().stream()
                            .anyMatch(f -> "name_text_description_text_brand_text_tags_text".equals(f.getKey())));

            if (!textIndexExists) {
                Document textIndex = new Document()
                        .append("name", "text")
                        .append("description", "text")
                        .append("brand", "text")
                        .append("tags", "text");

                IndexOptions indexOptions = new IndexOptions()
                        .name("product_text_search")
                        .weights(new Document()
                                .append("name", 10)
                                .append("description", 5)
                                .append("brand", 3)
                                .append("tags", 2));

                mongoTemplate.getCollection(mongoTemplate.getCollectionName(ProductDocument.class))
                        .createIndex(textIndex, indexOptions);

                log.info("Created text index on products collection: name(10), description(5), brand(3), tags(2)");
            } else {
                log.info("Text index already exists on products collection.");
            }
        } catch (Exception e) {
            log.warn("Could not ensure text indexes: {}. Spring Data MongoDB will handle this automatically.", e.getMessage());
        }
    }
}
