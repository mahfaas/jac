package com.shuld.jac.apigateway.security;

import com.shuld.jac.apigateway.exception.InvalidJwtException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
public class GatewayJwtService {

    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
    private static final String CLAIM_TOKEN_TYPE = "type";

    private final JwtProperties properties;
    private SecretKey key;

    public GatewayJwtService(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        if (properties.getSecret() == null || properties.getSecret().length() < 32) {
            throw new IllegalStateException("jwt.secret must be configured and at least 32 characters long");
        }
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public void validateAccessToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidJwtException("Token must not be blank");
        }
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            String actualType = claims.get(CLAIM_TOKEN_TYPE, String.class);
            if (!TOKEN_TYPE_ACCESS.equals(actualType)) {
                throw new InvalidJwtException("Expected an ACCESS token but got " + actualType);
            }
        } catch (InvalidJwtException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidJwtException("Invalid or expired token", e);
        }
    }
}
