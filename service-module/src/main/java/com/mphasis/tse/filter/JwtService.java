package com.mphasis.tse.filter;

import com.mphasis.tse.entity.User;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Service
public class JwtService {

    private Key key;

    @Value("${security.jwt.expiration-time}")
    private long expiration;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "security.jwt.secret must be configured. Application cannot start without a JWT signing secret.");
        }
        byte[] keyBytes = resolveSecretBytes(jwtSecret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT signing key loaded from configuration");
    }

    private byte[] resolveSecretBytes(String secret) {
        try {
            return Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ignored) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    public String generateToken(UserDetails user) {
        JwtBuilder builder = Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration));

        if (user instanceof User appUser) {
            if (appUser.getName() != null) {
                builder.claim("name", appUser.getName());
            }
        }

        return builder.signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            log.error("Failed to extract username from token: {}", e.getMessage());
            return null;
        }
    }

    public Date extractExpiration(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
        } catch (Exception e) {
            log.error("Failed to extract expiration from token: {}", e.getMessage());
            return null;
        }
    }

    public boolean isTokenExpired(String token) {
        Date expirationDate = extractExpiration(token);
        return expirationDate == null || expirationDate.before(new Date());
    }

    public boolean isTokenValid(String token, UserDetails user) {
        try {
            String username = extractUsername(token);
            if (username == null) {
                return false;
            }
            return username.equals(user.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return false;
        }
    }
}
