package com.makershub.auth.security;

import com.makershub.auth.enums.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret:defaultSecretKeyThatIsAtLeast32BytesLongForTesting}")
    private String jwtSecret;

    @Value("${jwt.access-expiration-ms:3600000}")
    private long accessExpiryMs;

    @Value("${jwt.refresh-expiration-ms:2592000000}")
    private long refreshExpiryMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, String phoneNumber, UserRole role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("phone", phoneNumber)
                .claim("role", role != null ? role.name() : "")
                .claim("type", "ACCESS")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessExpiryMs)))
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "REFRESH")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(refreshExpiryMs)))
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
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
