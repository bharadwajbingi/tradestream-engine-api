package com.mphasis.tse.filter;

import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private static final SecretKey TEST_SECRET_KEY =
            Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256);

    private static final long TEST_EXPIRATION = 1000 * 60 * 60; // 1 hour
    private static final String USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "key", TEST_SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "expiration", TEST_EXPIRATION);
    }

    @Test
    void testGenerateToken_andExtractUsername() {
        User user = new User(USERNAME, "password", Collections.emptyList());

        String token = jwtService.generateToken(user);

        assertNotNull(token);

        String extractedUsername = jwtService.extractUsername(token);

        assertEquals(USERNAME, extractedUsername);
    }


    @Test
    void testIsTokenValid_withValidToken() {
        User user = new User(USERNAME, "password", Collections.emptyList());

        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void testCorruptedToken_shouldThrowException() {
        User user = new User(USERNAME, "password", Collections.emptyList());

        String token = jwtService.generateToken(user);

        String corruptedToken = token.substring(0, token.length() - 2) + "xx";

        assertThrows(io.jsonwebtoken.security.SignatureException.class,
                () -> jwtService.extractUsername(corruptedToken));
    }

    @Test
    void testIsTokenValid_withDifferentUsername() {
        User user = new User(USERNAME, "password", Collections.emptyList());

        String token = jwtService.generateToken(user);

        User otherUser = new User("otheruser", "password", Collections.emptyList());

        assertFalse(jwtService.isTokenValid(token, otherUser));
    }

    @Test
    void testExpiredToken_shouldThrowException() {
        JwtService service = new JwtService();

        ReflectionTestUtils.setField(service, "key", TEST_SECRET_KEY);
        ReflectionTestUtils.setField(service, "expiration", 1L);

        User user = new User(USERNAME, "password", Collections.emptyList());

        String token = service.generateToken(user);

        assertThrows(io.jsonwebtoken.ExpiredJwtException.class,
                () -> service.extractUsername(token));
    }

    @Test
    void testMalformedToken_shouldThrowException() {
        String invalidToken = "this.is.not.valid";

        assertThrows(MalformedJwtException.class,
                () -> jwtService.extractUsername(invalidToken));
    }

    @Test
    void testInit_shouldGenerateKey() throws Exception {
        JwtService service = new JwtService();

        ReflectionTestUtils.setField(service, "expiration", TEST_EXPIRATION);

        service.init();

        Object key = ReflectionTestUtils.getField(service, "key");

        assertNotNull(key);
    }
}