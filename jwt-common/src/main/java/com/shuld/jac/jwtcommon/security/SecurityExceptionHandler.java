package com.shuld.jac.jwtcommon.security;

import com.shuld.jac.jwtcommon.SecurityErrorResponse;
import com.shuld.jac.jwtcommon.exception.InvalidJwtException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Ordered ahead of every service's own {@code @RestControllerAdvice}: Spring resolves
 * {@code @ExceptionHandler} methods by iterating advice beans in order and stopping at the
 * first bean with ANY matching handler (including a generic {@code Exception.class} catch-all)
 * — it does not keep searching other beans for a more specific match. Without this explicit
 * ordering, a service's own catch-all can shadow these more specific handlers depending on
 * unpredictable component-scan bean order.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(InvalidJwtException.class)
    public ResponseEntity<SecurityErrorResponse> handleInvalidJwt(InvalidJwtException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(SecurityErrorResponse.of(401, "Unauthorized", ex.getMessage()));
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<SecurityErrorResponse> handleMissingCredentials(AuthenticationCredentialsNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(SecurityErrorResponse.of(401, "Unauthorized", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<SecurityErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(SecurityErrorResponse.of(403, "Forbidden", ex.getMessage()));
    }
}
