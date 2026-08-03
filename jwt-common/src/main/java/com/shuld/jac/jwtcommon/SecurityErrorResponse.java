package com.shuld.jac.jwtcommon;

import java.time.LocalDateTime;

public record SecurityErrorResponse(LocalDateTime timestamp, int status, String error, String message) {

    public static SecurityErrorResponse of(int status, String error, String message) {
        return new SecurityErrorResponse(LocalDateTime.now(), status, error, message);
    }
}
