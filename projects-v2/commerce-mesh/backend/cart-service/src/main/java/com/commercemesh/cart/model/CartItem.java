package com.commercemesh.cart.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * POJO nested inside Cart — not a standalone MongoDB document.
 */
public final class CartItem {

    private String productId;
    private String name;
    private BigDecimal price;
    private int quantity;
    private String image;

    public CartItem() {
    }

    public CartItem(String productId, String name, BigDecimal price, int quantity, String image) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.image = image;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CartItem that)) return false;
        return quantity == that.quantity
                && Objects.equals(productId, that.productId)
                && Objects.equals(name, that.name)
                && Objects.equals(price, that.price)
                && Objects.equals(image, that.image);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, name, price, quantity, image);
    }

    @Override
    public String toString() {
        return "CartItem{productId='" + productId + "', name='" + name + "', price=" + price
                + ", quantity=" + quantity + ", image='" + image + "'}";
    }
}
