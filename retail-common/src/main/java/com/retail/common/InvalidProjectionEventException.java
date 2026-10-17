package com.retail.common;

public class InvalidProjectionEventException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidProjectionEventException(String message) {
        super(message);
    }

    public InvalidProjectionEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
