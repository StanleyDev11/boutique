package com.example.boutique.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class FeatureUnavailableException extends RuntimeException {
    public FeatureUnavailableException(String message) {
        super(message);
    }
}
