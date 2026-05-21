package com.mphasis.tse.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Service
public class JwtService {

    private Key key;

    @Value("${security.jwt.expiration-time}")
    private long expiration;

    @Value("${security.jwt.secret:}")
    private String jwtSecret;

    @PostConstruct
    public void init() throws Exception {
        if (jwtSecret != null && !jwtSecret.isBlank()) {
            byte[] keyBytes = resolveSecretBytes(jwtSecret);
            this.key = Keys.hmacShaKeyFor(keyBytes);
            log.info("JWT signing key loaded from configuration");
            return;
        }

        byte[] generatedDevKey = "trade-stream-engine-dev-secret-change-me".getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(generatedDevKey);
        log.warn("security.jwt.secret is not configured; using development fallback key");

    }

    private byte[] resolveSecretBytes(String secret) {
        try {
            return Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ignored) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    public String generateToken(org.springframework.security.core.userdetails.UserDetails user) {
        io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration));

        if (user instanceof com.mphasis.tse.entity.User) {
            com.mphasis.tse.entity.User appUser = (com.mphasis.tse.entity.User) user;
            if (appUser.getName() != null) {
                builder.claim("name", appUser.getName());
            }
        }

        return builder.signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean isTokenValid(String token, org.springframework.security.core.userdetails.UserDetails user) {
        return extractUsername(token).equals(user.getUsername());
    }
}
