package io.github.abhishekghosh.jenkinshelper.model;


import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UserException extends Exception {
    int httpStatusCode;
    String message;

    public UserException(HttpStatus httpStatusCode, String message) {
        super(message);
        this.httpStatusCode = httpStatusCode.value();
        this.message = message;
    }

}
