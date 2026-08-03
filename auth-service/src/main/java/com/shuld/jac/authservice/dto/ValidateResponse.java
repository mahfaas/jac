package com.shuld.jac.authservice.dto;

public class ValidateResponse {

    private final boolean valid;
    private final Long userId;
    private final String role;

    public ValidateResponse(boolean valid, Long userId, String role) {
        this.valid = valid;
        this.userId = userId;
        this.role = role;
    }

    public boolean isValid() {
        return valid;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }
}
