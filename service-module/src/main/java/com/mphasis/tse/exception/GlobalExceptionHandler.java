package com.mphasis.tse.exception;

import com.mphasis.tse.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

}
