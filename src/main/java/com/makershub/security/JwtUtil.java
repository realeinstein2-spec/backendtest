package com.makershub.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtil {

    @Value("${makershub.jwt.secret}")
    private String jwtSecret;

    @Value("${makershub.jwt.access-expiry-ms:1800000}")
    private long accessExpiryMs;

    @Value("${makershub.jwt.refresh-expiry-ms:2592000000}")
    private long refreshExpiryMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(UserDetailsImpl userDetails) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userDetails.getId().toString())
                .claim("phone", userDetails.getPhoneNumber())
                .claim("role", userDetails.getRole().name())
                .claim("type", "ACCESS")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessExpiryMs)))
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    public String generateRefreshToken(UserDetailsImpl userDetails) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userDetails.getId().toString())
                .claim("type", "REFRESH")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(refreshExpiryMs)))
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token, String expectedType) {
        try {
            Claims claims = parseToken(token);
            return !claims.getExpiration().before(new Date())
                    && expectedType.equals(claims.get("type", String.class));
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseToken(token).getSubject());
    }

    public Instant getAccessExpiry() {
        return Instant.now().plusMillis(accessExpiryMs);
    }

    public Instant getRefreshExpiry() {
        return Instant.now().plusMillis(refreshExpiryMs);
    }
}
