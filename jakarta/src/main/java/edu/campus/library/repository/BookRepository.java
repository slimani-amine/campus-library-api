package edu.campus.library.repository;

import edu.campus.library.domain.Book;

import java.util.List;
import java.util.Optional;

/** Where books are stored. The service only knows this interface, not the implementation. */
public interface BookRepository {

    List<Book> findAll();

    Optional<Book> find(long id);

    /** Inserts the book if its id is null (a new id is assigned), otherwise replaces it. */
    Book save(Book book);

    /** Returns true if a book was removed. */
    boolean delete(long id);
}
