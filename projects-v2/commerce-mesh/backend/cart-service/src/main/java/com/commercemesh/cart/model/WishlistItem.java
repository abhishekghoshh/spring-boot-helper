package com.commercemesh.cart.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * POJO nested inside Wishlist — not a standalone MongoDB document.
 */
public final class WishlistItem {

    private String productId;
    private String name;
    private BigDecimal price;
    private String image;
    private Instant addedAt;

    public WishlistItem() {
    }

    public WishlistItem(String productId, String name, BigDecimal price, String image, Instant addedAt) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.image = image;
        this.addedAt = addedAt;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Instant getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Instant addedAt) {
        this.addedAt = addedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WishlistItem that)) return false;
        return Objects.equals(productId, that.productId)
                && Objects.equals(name, that.name)
                && Objects.equals(price, that.price)
                && Objects.equals(image, that.image)
                && Objects.equals(addedAt, that.addedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, name, price, image, addedAt);
    }

    @Override
    public String toString() {
        return "WishlistItem{productId='" + productId + "', name='" + name + "', price=" + price
                + ", image='" + image + "', addedAt=" + addedAt + "}";
    }
}
