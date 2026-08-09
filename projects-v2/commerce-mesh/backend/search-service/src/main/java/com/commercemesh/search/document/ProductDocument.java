package com.commercemesh.search.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.TextScore;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "products")
public class ProductDocument {

    @Id
    private String id;

    @TextIndexed(weight = 10)
    @Field("name")
    private String name;

    @TextIndexed(weight = 5)
    @Field("description")
    private String description;

    @TextIndexed(weight = 3)
    @Field("brand")
    private String brand;

    @Field("category")
    private String category;

    @Field("price")
    private BigDecimal price;

    @Field("image")
    private String image;

    @Field("averageRating")
    private double averageRating;

    @Field("reviewCount")
    private int reviewCount;

    @TextIndexed(weight = 2)
    @Field("tags")
    private List<String> tags;

    @Field("isActive")
    private boolean isActive;

    @Field("createdAt")
    private Instant createdAt;

    @TextScore
    private Float textScore;

    public ProductDocument() {
    }

    public ProductDocument(String id, String name, String description, String brand, String category,
                           BigDecimal price, String image, double averageRating, int reviewCount,
                           List<String> tags, boolean isActive, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.brand = brand;
        this.category = category;
        this.price = price;
        this.image = image;
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Float getTextScore() {
        return textScore;
    }

    public void setTextScore(Float textScore) {
        this.textScore = textScore;
    }
}
