package com.shuld.jac.jwtcommon;

import com.shuld.jac.jwtcommon.exception.InvalidJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-key-with-at-least-32-characters!!");
        properties.setAccessTokenTtlMinutes(15);
        properties.setRefreshTokenTtlDays(7);

        jwtService = new JwtService(properties);
        jwtService.init();
    }

    @Test
    void generateAndParseAccessToken_roundTripsUserIdAndRole() {
        String token = jwtService.generateAccessToken(42L, "ADMIN");

        JwtClaims claims = jwtService.parseAccessToken(token);

        assertThat(claims.userId()).isEqualTo(42L);
        assertThat(claims.role()).isEqualTo("ADMIN");
    }

    @Test
    void generateAndParseRefreshToken_roundTripsUserIdAndRole() {
        String token = jwtService.generateRefreshToken(7L, "USER");

        JwtClaims claims = jwtService.parseRefreshToken(token);

        assertThat(claims.userId()).isEqualTo(7L);
        assertThat(claims.role()).isEqualTo("USER");
    }

    @Test
    void parseAccessToken_rejectsRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken(1L, "USER");

        assertThatThrownBy(() -> jwtService.parseAccessToken(refreshToken))
                .isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void parseRefreshToken_rejectsAccessToken() {
        String accessToken = jwtService.generateAccessToken(1L, "USER");

        assertThatThrownBy(() -> jwtService.parseRefreshToken(accessToken))
                .isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void parseAccessToken_rejectsTamperedToken() {
        String token = jwtService.generateAccessToken(1L, "USER");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtService.parseAccessToken(tampered))
                .isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void parseAccessToken_rejectsBlankToken() {
        assertThatThrownBy(() -> jwtService.parseAccessToken(" "))
                .isInstanceOf(InvalidJwtException.class);
    }
}
