package edu.campus.library.domain;

/** A library member who can borrow books. */
public record Member(Long id, String name, String email) {
}
