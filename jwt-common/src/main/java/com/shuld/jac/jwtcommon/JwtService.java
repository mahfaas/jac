package com.shuld.jac.jwtcommon;

import com.shuld.jac.jwtcommon.exception.InvalidJwtException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "type";

    private final JwtProperties properties;
    private SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        if (properties.getSecret() == null || properties.getSecret().length() < 32) {
            throw new IllegalStateException("jwt.secret must be configured and at least 32 characters long");
        }
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String role) {
        return buildToken(userId, role, TOKEN_TYPE_ACCESS, Duration.ofMinutes(properties.getAccessTokenTtlMinutes()));
    }

    public String generateRefreshToken(Long userId, String role) {
        return buildToken(userId, role, TOKEN_TYPE_REFRESH, Duration.ofDays(properties.getRefreshTokenTtlDays()));
    }

    public JwtClaims parseAccessToken(String token) {
        return parse(token, TOKEN_TYPE_ACCESS);
    }

    public JwtClaims parseRefreshToken(String token) {
        return parse(token, TOKEN_TYPE_REFRESH);
    }

    private String buildToken(Long userId, String role, String tokenType, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    private JwtClaims parse(String token, String expectedType) {
        if (token == null || token.isBlank()) {
            throw new InvalidJwtException("Token must not be blank");
        }
        try {
            Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            Claims claims = jws.getPayload();

            String actualType = claims.get(CLAIM_TOKEN_TYPE, String.class);
            if (!expectedType.equals(actualType)) {
                throw new InvalidJwtException("Expected a " + expectedType + " token but got " + actualType);
            }

            Long userId = Long.valueOf(claims.getSubject());
            String role = claims.get(CLAIM_ROLE, String.class);
            return new JwtClaims(userId, role);
        } catch (InvalidJwtException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidJwtException("Invalid or expired token", e);
        }
    }
}
