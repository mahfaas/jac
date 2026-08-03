package com.shuld.jac.authservice.service;

import com.shuld.jac.authservice.dto.LoginRequest;
import com.shuld.jac.authservice.dto.RefreshRequest;
import com.shuld.jac.authservice.dto.RegisterRequest;
import com.shuld.jac.authservice.dto.TokenResponse;
import com.shuld.jac.authservice.entity.Credential;
import com.shuld.jac.authservice.entity.Role;
import com.shuld.jac.authservice.exception.InvalidCredentialsException;
import com.shuld.jac.authservice.repository.CredentialRepository;
import com.shuld.jac.jwtcommon.JwtClaims;
import com.shuld.jac.jwtcommon.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CredentialRepository credentialRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(credentialRepository, passwordEncoder, jwtService);
    }

    private Credential sampleCredential() {
        Credential credential = new Credential();
        credential.setId(1L);
        credential.setUserId(100L);
        credential.setLogin("john.doe");
        credential.setPasswordHash("hashed");
        credential.setRole(Role.USER);
        return credential;
    }

    @Test
    void register_hashesPasswordAndPersistsCredential() {
        RegisterRequest request = new RegisterRequest();
        request.setUserId(100L);
        request.setLogin("john.doe");
        request.setPassword("plaintext123");
        request.setRole(Role.USER);

        when(passwordEncoder.encode("plaintext123")).thenReturn("hashed");
        when(credentialRepository.save(any(Credential.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.register(request);

        assertThat(response.getUserId()).isEqualTo(100L);
        assertThat(response.getLogin()).isEqualTo("john.doe");
        assertThat(response.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void login_validCredentials_returnsTokens() {
        Credential credential = sampleCredential();
        LoginRequest request = new LoginRequest();
        request.setLogin("john.doe");
        request.setPassword("plaintext123");

        when(credentialRepository.findByLogin("john.doe")).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("plaintext123", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(100L, "USER")).thenReturn("access-token");
        when(jwtService.generateRefreshToken(100L, "USER")).thenReturn("refresh-token");

        TokenResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void login_unknownLogin_throwsInvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setLogin("nobody");
        request.setPassword("whatever");

        when(credentialRepository.findByLogin("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        Credential credential = sampleCredential();
        LoginRequest request = new LoginRequest();
        request.setLogin("john.doe");
        request.setPassword("wrong");

        when(credentialRepository.findByLogin("john.doe")).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refresh_validToken_reissuesTokensWithCurrentRole() {
        Credential credential = sampleCredential();
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("refresh-token");

        when(jwtService.parseRefreshToken("refresh-token")).thenReturn(new JwtClaims(100L, "USER"));
        when(credentialRepository.findByUserId(100L)).thenReturn(Optional.of(credential));
        when(jwtService.generateAccessToken(100L, "USER")).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(100L, "USER")).thenReturn("new-refresh-token");

        TokenResponse response = authService.refresh(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void refresh_accountDeleted_throwsInvalidCredentials() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("refresh-token");

        when(jwtService.parseRefreshToken("refresh-token")).thenReturn(new JwtClaims(100L, "USER"));
        when(credentialRepository.findByUserId(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void validate_delegatesToJwtServiceAndReturnsClaims() {
        when(jwtService.parseAccessToken("access-token")).thenReturn(new JwtClaims(100L, "ADMIN"));

        var response = authService.validate("access-token");

        assertThat(response.isValid()).isTrue();
        assertThat(response.getUserId()).isEqualTo(100L);
        assertThat(response.getRole()).isEqualTo("ADMIN");
    }
}
