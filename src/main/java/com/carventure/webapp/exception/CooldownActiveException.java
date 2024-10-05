package com.carventure.webapp.exception;

public class CooldownActiveException extends RuntimeException {
    public CooldownActiveException(String message) {
        super(message);
    }
}
