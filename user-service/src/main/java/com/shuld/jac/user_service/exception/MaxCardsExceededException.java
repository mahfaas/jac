package com.shuld.jac.user_service.exception;

public class MaxCardsExceededException extends RuntimeException {
    public MaxCardsExceededException(String message) {
        super(message);
    }
}