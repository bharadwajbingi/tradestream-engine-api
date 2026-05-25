package com.mphasis.tse.exception;

import com.mphasis.tse.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileNotFoundException(FileNotFoundException ex){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body
                ( new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.name(),HttpStatus.INTERNAL_SERVER_ERROR.value(),ex.getMessage(),
                null
        ));
    }

    @ExceptionHandler(InvalidFileFormatException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidFormat(InvalidFileFormatException ex){

        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body
                ( new ApiResponse<>(HttpStatus.BAD_REQUEST.name(),HttpStatus.BAD_REQUEST.value(),ex.getMessage(),
                        null
                ));
    }

    @ExceptionHandler(EmptyFileException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmptyFile(EmptyFileException ex){

        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body
                ( new ApiResponse<>(HttpStatus.BAD_REQUEST.name(),HttpStatus.BAD_REQUEST.value(),ex.getMessage(),
                        null
                ));
    }
    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateUserException(DuplicateUserException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiResponse<>(
                        HttpStatus.BAD_REQUEST.name(),
                        HttpStatus.BAD_REQUEST.value(),
                        ex.getMessage(),
                        null
                )
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ApiResponse<>(
                        HttpStatus.FORBIDDEN.name(),
                        HttpStatus.FORBIDDEN.value(),
                        ex.getMessage(),
                        null
                )
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiResponse<>(
                        HttpStatus.BAD_REQUEST.name(),
                        HttpStatus.BAD_REQUEST.value(),
                        ex.getMessage(),
                        null
                )
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiResponse<>(
                        HttpStatus.BAD_REQUEST.name(),
                        HttpStatus.BAD_REQUEST.value(),
                        errorMessage,
                        null
                )
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new ApiResponse<>(
                        HttpStatus.UNAUTHORIZED.name(),
                        HttpStatus.UNAUTHORIZED.value(),
                        ex.getMessage(),
                        null
                )
        );
    }

}
