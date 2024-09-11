package com.ag.in.rest.client.exception;

import org.springframework.http.HttpStatus;

public class RestCallException extends Exception {
    private String message;
    private String description;
    private HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public RestCallException(String message) {
        super(message);
        this.message = message;
    }

    public RestCallException(String message, String description) {
        super(message);
        this.message = message;
        this.description = description;
    }

    public RestCallException(Exception exception) {
        super(exception);
    }

    public RestCallException(String message, HttpStatus httpStatus, String description) {
        super(message);
        this.message = message;
        this.description = description;
        this.httpStatus = httpStatus;
    }

    public RestCallException(String message, String description, HttpStatus httpStatus, Exception exception) {
        super(exception);
        this.message = message;
        this.description = description;
        this.httpStatus = httpStatus;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

}
