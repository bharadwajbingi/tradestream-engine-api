package com.mphasis.tse.filter;

import io.jsonwebtoken.Claims;
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
public class ExportTokenService {

    private Key key;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @PostConstruct
    public void init() throws Exception {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "security.jwt.secret must be configured. Application cannot start without a JWT signing secret.");
        }
        byte[] keyBytes = resolveSecretBytes(jwtSecret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        log.info("Export Token signing key loaded from configuration");
    }

    private byte[] resolveSecretBytes(String secret) {
        try {
            return Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ignored) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    public String generateExportToken(String email) {
        long fiveMinutes = 5 * 60 * 1000; // 5 minutes in ms
        return Jwts.builder()
                .setSubject(email)
                .claim("type", "export")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + fiveMinutes))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String validateExportTokenAndGetEmail(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String type = claims.get("type", String.class);
            if (!"export".equals(type)) {
                log.warn("JWT is not of type export");
                return null;
            }

            return claims.getSubject();
        } catch (Exception e) {
            log.warn("Invalid export token: {}", e.getMessage());
            return null;
        }
    }
}
