package com.mphasis.tse.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Service
public class JwtService {

    private Key key;

    @Value("${security.jwt.expiration-time}")
    private long expiration;

    @PostConstruct
    public void init() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        keyGen.init(256);

        SecretKey secretKey = keyGen.generateKey();
        this.key = secretKey;

        String base64UrlKey = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(secretKey.getEncoded());

        log.info("Base64URL Key: {}", base64UrlKey);

    }

    public String generateToken(org.springframework.security.core.userdetails.UserDetails user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
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