package com.committr.backend.crypto;

public class TokenEncryptionException extends RuntimeException {

    public TokenEncryptionException(String message) {
        super(message);
    }

    public TokenEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
