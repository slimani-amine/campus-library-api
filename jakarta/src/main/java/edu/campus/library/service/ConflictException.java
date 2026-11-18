package edu.campus.library.service;

/** Thrown when a request breaks a business rule. Mapped to 409 by the api layer. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
