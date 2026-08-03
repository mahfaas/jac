package com.shuld.jac.authservice.service;

import com.shuld.jac.authservice.dto.CredentialResponse;
import com.shuld.jac.authservice.dto.LoginRequest;
import com.shuld.jac.authservice.dto.RefreshRequest;
import com.shuld.jac.authservice.dto.RegisterRequest;
import com.shuld.jac.authservice.dto.TokenResponse;
import com.shuld.jac.authservice.dto.ValidateResponse;
import com.shuld.jac.authservice.entity.Credential;
import com.shuld.jac.authservice.exception.InvalidCredentialsException;
import com.shuld.jac.authservice.repository.CredentialRepository;
import com.shuld.jac.jwtcommon.JwtClaims;
import com.shuld.jac.jwtcommon.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(CredentialRepository credentialRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public CredentialResponse register(RegisterRequest request) {
        Credential credential = new Credential();
        credential.setUserId(request.getUserId());
        credential.setLogin(request.getLogin());
        credential.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        credential.setRole(request.getRole());

        Credential saved = credentialRepository.save(credential);
        return toResponse(saved);
    }

    public TokenResponse login(LoginRequest request) {
        Credential credential = credentialRepository.findByLogin(request.getLogin())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid login or password"));

        if (!passwordEncoder.matches(request.getPassword(), credential.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid login or password");
        }

        return issueTokens(credential);
    }

    public TokenResponse refresh(RefreshRequest request) {
        JwtClaims claims = jwtService.parseRefreshToken(request.getRefreshToken());

        Credential credential = credentialRepository.findByUserId(claims.userId())
                .orElseThrow(() -> new InvalidCredentialsException("Account no longer exists"));

        return issueTokens(credential);
    }

    public ValidateResponse validate(String token) {
        JwtClaims claims = jwtService.parseAccessToken(token);
        return new ValidateResponse(true, claims.userId(), claims.role());
    }

    private TokenResponse issueTokens(Credential credential) {
        String role = credential.getRole().name();
        String accessToken = jwtService.generateAccessToken(credential.getUserId(), role);
        String refreshToken = jwtService.generateRefreshToken(credential.getUserId(), role);
        return new TokenResponse(accessToken, refreshToken);
    }

    private CredentialResponse toResponse(Credential credential) {
        CredentialResponse response = new CredentialResponse();
        response.setId(credential.getId());
        response.setUserId(credential.getUserId());
        response.setLogin(credential.getLogin());
        response.setRole(credential.getRole());
        response.setCreatedAt(credential.getCreatedAt());
        return response;
    }
}
