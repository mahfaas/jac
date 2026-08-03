package com.shuld.jac.jwtcommon.security;

public class JwtUserPrincipal {

    private final Long userId;
    private final String role;

    public JwtUserPrincipal(Long userId, String role) {
        this.userId = userId;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }
}
