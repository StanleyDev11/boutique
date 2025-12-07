package com.example.boutique.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(annotations = org.springframework.web.bind.annotation.RestController.class)
public class RestExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        logger.warn("Violation de contrainte de données : {}", e.getMessage());
        logger.debug("Cause racine de la violation: {}", e.getMostSpecificCause().getMessage());
        String errorMessage = "L'opération a échoué en raison d'un conflit de données. " +
                "Cela peut être dû à la duplication d'une valeur qui doit être unique (comme un code-barres ou un nom) " +
                "ou à la tentative de supprimer un élément utilisé par d'autres enregistrements.";
        return new ResponseEntity<>(Map.of("error", errorMessage), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        logger.warn("Argument illégal : {}", e.getMessage());
        return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAllExceptions(Exception e) {
        logger.error("Erreur inattendue dans une API REST", e);
        return new ResponseEntity<>(Map.of("error", "Une erreur technique inattendue est survenue. L'administrateur a été notifié."), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
