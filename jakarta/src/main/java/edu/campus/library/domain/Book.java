package edu.campus.library.domain;

public record Book(Long id, String title, String author, String isbn, boolean available) {
}
