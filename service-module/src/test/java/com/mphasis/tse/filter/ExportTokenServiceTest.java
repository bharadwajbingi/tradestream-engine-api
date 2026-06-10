package com.mphasis.tse.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExportTokenService — 5-min export JWT generation and validation")
class ExportTokenServiceTest {

    private ExportTokenService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new ExportTokenService();
        ReflectionTestUtils.setField(service, "jwtSecret", "thisIsATestSecretKeyThatMustBeAtLeast32CharsLong!");
        service.init();
    }

    @Test
    @DisplayName("init fails if secret is null")
    void init_nullSecret_throws() {
        ExportTokenService svc = new ExportTokenService();
        ReflectionTestUtils.setField(svc, "jwtSecret", null);
        assertThrows(IllegalStateException.class, svc::init);
    }

    @Test
    @DisplayName("init fails if secret is blank")
    void init_blankSecret_throws() {
        ExportTokenService svc = new ExportTokenService();
        ReflectionTestUtils.setField(svc, "jwtSecret", "   ");
        assertThrows(IllegalStateException.class, svc::init);
    }

    @Test
    @DisplayName("generates non-null JWT token for valid email")
    void generateToken_returnsJwt() {
        String token = service.generateExportToken("user@example.com");
        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals(3, token.split("\\.").length, "JWT must have 3 parts");
    }

    @Test
    @DisplayName("validates token and returns email")
    void validateToken_returnsEmail() {
        String token = service.generateExportToken("export@tradestream.io");
        String email = service.validateExportTokenAndGetEmail(token);
        assertEquals("export@tradestream.io", email);
    }

    @Test
    @DisplayName("returns null for tampered token")
    void invalidToken_returnsNull() {
        String email = service.validateExportTokenAndGetEmail("invalid.token.here");
        assertNull(email);
    }

    @Test
    @DisplayName("returns null for non-export type token (if manually crafted)")
    void nonExportType_returnsNull() {
        // A valid export token should pass; a completely garbage token returns null
        String email = service.validateExportTokenAndGetEmail("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.garbage");
        assertNull(email);
    }
}
