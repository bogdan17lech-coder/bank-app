package com.example.bank.rest.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            Map<String, String> e = new HashMap<>();
            e.put("field", fe.getField());
            e.put("message", fe.getDefaultMessage());
            errors.add(e);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("status", 400);
        body.put("error", "Bad Request");
        body.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>>
    handleRse(org.springframework.web.server.ResponseStatusException ex) {

        org.springframework.http.HttpStatusCode sc = ex.getStatusCode();
        org.springframework.http.HttpStatus hs = org.springframework.http.HttpStatus.resolve(sc.value());
        String reason = (hs != null) ? hs.getReasonPhrase() : "Error";

        var body = new java.util.HashMap<String, Object>();
        body.put("status", sc.value());
        body.put("error", reason);
        body.put("message", ex.getReason());

        return org.springframework.http.ResponseEntity
                .status(sc)
                .body(body);
    }
}
