package io.github.abhishekghoshh.core.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiException extends Exception {
    private final String message;
    private final int statusCode;

    public ApiException(String message, int statusCode) {
        super(message);
        this.message = message;
        this.statusCode = statusCode;
    }
}
