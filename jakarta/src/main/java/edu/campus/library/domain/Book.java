package edu.campus.library.domain;

/** A book in the catalogue. {@code available} is false while the book is on loan. */
public record Book(Long id, String title, String author, String isbn, boolean available) {
}
