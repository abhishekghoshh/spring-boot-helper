package io.github.abhishekghosh.core.advice;

import io.github.abhishekghosh.core.dto.ErrorDTO;
import io.github.abhishekghosh.core.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ApiException.class})
    public ResponseEntity<ErrorDTO> handleGlobalException(ApiException exc) {
        ErrorDTO error = new ErrorDTO(exc.getMessage());
        return ResponseEntity
                .status(HttpStatus.valueOf(exc.getStatusCode()))
                .body(error);
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<ErrorDTO> handleGlobalException(Exception exc) {
        ErrorDTO error = new ErrorDTO(exc.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}
