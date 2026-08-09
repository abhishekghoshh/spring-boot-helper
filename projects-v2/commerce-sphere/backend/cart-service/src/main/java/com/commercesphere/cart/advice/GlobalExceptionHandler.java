package com.commercesphere.cart.advice;
import org.springframework.http.HttpStatus;import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
@RestControllerAdvice
public class GlobalExceptionHandler {
    record ErrorResponse(String error, String message, int status, Instant timestamp) { ErrorResponse(String error, String message, int status) { this(error, message, status, Instant.now()); } }
    @ExceptionHandler(RuntimeException.class) public ResponseEntity<ErrorResponse> handle(RuntimeException ex) { return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("BAD_REQUEST", ex.getMessage(), 400)); }
    @ExceptionHandler(Exception.class) public ResponseEntity<ErrorResponse> handleGen(Exception ex) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("INTERNAL_ERROR", ex.getMessage(), 500)); }
}
