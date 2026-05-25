package com.mphasis.tse.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "expiration", 3600000L); // 1 hour expiration
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "eW91ci1zdXBlci1zZWNyZXQtMzJjaGFyLWtleS1nb2VzLWhlcmUtbm93"); // Base64 encoded mock key
        jwtService.init();

        userDetails = new User("testuser@example.com", "password", Collections.emptyList());
    }

    @Test
    void testGenerateAndValidateValidToken() {
        String token = jwtService.generateToken(userDetails);
        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token, userDetails));
        assertEquals("testuser@example.com", jwtService.extractUsername(token));
        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    void testExpiredTokenValidation() throws Exception {
        // Set very short expiration to generate expired token
        JwtService shortLivedService = new JwtService();
        ReflectionTestUtils.setField(shortLivedService, "expiration", -1000L); // Expired 1 second ago
        ReflectionTestUtils.setField(shortLivedService, "jwtSecret", "eW91ci1zdXBlci1zZWNyZXQtMzJjaGFyLWtleS1nb2VzLWhlcmUtbm93");
        shortLivedService.init();

        String expiredToken = shortLivedService.generateToken(userDetails);
        assertNotNull(expiredToken);
        assertFalse(shortLivedService.isTokenValid(expiredToken, userDetails));
        assertTrue(shortLivedService.isTokenExpired(expiredToken));
    }

    @Test
    void testMalformedTokenValidation() {
        String malformedToken = "invalidHeader.invalidPayload.invalidSignature";
        assertFalse(jwtService.isTokenValid(malformedToken, userDetails));
        assertNull(jwtService.extractUsername(malformedToken));
        assertTrue(jwtService.isTokenExpired(malformedToken));
    }

    @Test
    void testTamperedTokenValidation() {
        String originalToken = jwtService.generateToken(userDetails);
        // Tamper with the token string
        String tamperedToken = originalToken + "tamper";
        assertFalse(jwtService.isTokenValid(tamperedToken, userDetails));
        assertNull(jwtService.extractUsername(tamperedToken));
    }
}