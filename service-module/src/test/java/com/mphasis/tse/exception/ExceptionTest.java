package com.mphasis.tse.exception;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionTest {

    @ParameterizedTest
    @CsvSource({
            "DuplicateUserException, Duplicate error",
            "EmptyFileException, Empty file",
            "FileNotFoundException, File missing",
            "InvalidFileFormatException, Invalid format"
    })
    void testCustomExceptions(String className, String message) throws Exception {

        Class<?> clazz = Class.forName("com.mphasis.tse.exception." + className);

        RuntimeException ex = (RuntimeException)
                clazz.getConstructor(String.class).newInstance(message);

        assertEquals(message, ex.getMessage());
    }
}
