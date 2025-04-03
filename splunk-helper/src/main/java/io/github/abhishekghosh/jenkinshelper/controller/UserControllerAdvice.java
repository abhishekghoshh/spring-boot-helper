package io.github.abhishekghosh.jenkinshelper.controller;


import io.github.abhishekghosh.jenkinshelper.model.UserException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class UserControllerAdvice extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(UserControllerAdvice.class);

    @ExceptionHandler(UserException.class)
    public ResponseEntity<?> handleRestCallException(
            UserException ex, HttpServletRequest request) {
        log.error("Error in controller advisor is {}", ex.getMessage());
        return ResponseEntity
                .status(ex.getHttpStatusCode())
                .body(
                        Map.of(
                                "statusCode", ex.getHttpStatusCode(),
                                "message", ex.getMessage()
                        )
                );
    }

}
