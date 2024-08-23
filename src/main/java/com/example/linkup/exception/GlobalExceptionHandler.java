package com.example.linkup.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public String runTimeException(RuntimeException runtimeException) {
        return runtimeException.getMessage();
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<String> runTimeException(CustomException runtimeException) {
        return ResponseEntity.status(runtimeException.getHttpStatus()).body(runtimeException.getMessage());
    }
}
