package com.commercesphere.inventory.advice;
import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
import java.time.Instant;
@RestControllerAdvice
public class GlobalExceptionHandler {
    record ErrorResponse(String error, String message, int status, Instant t) { ErrorResponse(String e, String m, int s) { this(e, m, s, Instant.now()); } }
    @ExceptionHandler(RuntimeException.class) public ResponseEntity<ErrorResponse> h(RuntimeException ex) { return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("BAD_REQUEST", ex.getMessage(), 400)); }
}
