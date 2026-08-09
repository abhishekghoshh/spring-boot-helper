package com.commercemesh.cart.exception;

/**
 * Thrown when a requested resource (Cart, Wishlist, item) is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
