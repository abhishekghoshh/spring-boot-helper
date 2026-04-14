package io.github.abhishekghoshh.aws.core.controller;


import io.github.abhishekghoshh.aws.core.dto.ErrorDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({ApiException.class})
    public ResponseEntity<ErrorDTO> handleGlobalException(ApiException exc) {
        logger.error("API Exception: {}", exc.getMessage(), exc);
        ErrorDTO error = new ErrorDTO(exc.getMessage());
        return ResponseEntity
                .status(HttpStatus.valueOf(exc.getStatusCode()))
                .body(error);
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<ErrorDTO> handleGlobalException(Exception exc) {
        logger.error("Unexpected Exception: {}", exc.getMessage(), exc);
        ErrorDTO error = new ErrorDTO(exc.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}
