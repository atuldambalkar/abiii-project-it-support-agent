package com.example.itsupport.identity;

public class IdentityDelegationException extends RuntimeException {

    public IdentityDelegationException(String message) {
        super(message);
    }

    public IdentityDelegationException(String message, Throwable cause) {
        super(message, cause);
    }
}
