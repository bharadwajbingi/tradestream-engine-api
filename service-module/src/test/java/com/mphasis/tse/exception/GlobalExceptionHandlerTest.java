package com.mphasis.tse.exception;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.ResponseEntity;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @ParameterizedTest
    @CsvSource({
            "handleFileNotFoundException, FileNotFoundException, 500",
            "handleInvalidFormat, InvalidFileFormatException, 400",
            "handleEmptyFile, EmptyFileException, 400",
            "handleDuplicateUserException, DuplicateUserException, 400"
    })
    void testHandlers(String methodName, String exceptionName, int expectedStatus) throws Exception {

        Class<?> exClass = Class.forName("com.mphasis.tse.exception." + exceptionName);
        RuntimeException ex =
                (RuntimeException) exClass.getConstructor(String.class).newInstance("error");

        Method method = GlobalExceptionHandler.class
                .getMethod(methodName, exClass);

        ResponseEntity<?> response =
                (ResponseEntity<?>) method.invoke(handler, ex);

        assertEquals(expectedStatus, response.getStatusCode().value());
    }
}

