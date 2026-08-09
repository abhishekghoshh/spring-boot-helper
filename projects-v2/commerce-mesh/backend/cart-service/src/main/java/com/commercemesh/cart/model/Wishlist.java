package com.commercemesh.cart.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

/**
 * MongoDB document representing a user's wishlist.
 */
@Document(collection = "wishlists")
public class Wishlist {

    @MongoId
    private String id;

    @Indexed(unique = true)
    private String userId;

    private List<WishlistItem> items = new ArrayList<>();

    private Instant createdAt;

    private Instant updatedAt;

    public Wishlist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Wishlist(String userId) {
        this();
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<WishlistItem> getItems() {
        return items;
    }

    public void setItems(List<WishlistItem> items) {
        this.items = items;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
