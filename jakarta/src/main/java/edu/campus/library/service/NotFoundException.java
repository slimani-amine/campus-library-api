package edu.campus.library.service;

/** Thrown when an id does not match anything. Mapped to 404 by the api layer. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
