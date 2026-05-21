package com.study.study_platform.exception;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<?> handleUserExists(UserAlreadyExistsException ex) {

        Map<String, Object> error = new HashMap<>();
        error.put("status", 409);
        error.put("error", "CONFLICT");
        error.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex) {

        Map<String, Object> error = new HashMap<>();
        error.put("status", 404);
        error.put("error", "NOT_FOUND");
        error.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ObjectiveNotFoundException.class)
    public ResponseEntity<?> handleObjectiveNotFound(ObjectiveNotFoundException ex) {

        Map<String, Object> error = new HashMap<>();
        error.put("status", 404);
        error.put("error", "NOT_FOUND");
        error.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {

        Map<String, Object> error = new HashMap<>();
        error.put("status", 400);
        error.put("error", "BAD_REQUEST");
        error.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneral(Exception ex) {

        Map<String, Object> error = new HashMap<>();
        error.put("status", 500);
        error.put("error", "INTERNAL_SERVER_ERROR");
        error.put("message", "Internal error");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex
    ) {

        String message = ex.getBindingResult()
                .getFieldError()
                .getDefaultMessage();

        Map<String, Object> response = new HashMap<>();

        response.put("error", "VALIDATION_ERROR");
        response.put("message", message);
        response.put("status", 400);

        return ResponseEntity.badRequest().body(response);
    }
}