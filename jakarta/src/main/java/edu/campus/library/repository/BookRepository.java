package edu.campus.library.repository;

import edu.campus.library.domain.Book;

import java.util.List;
import java.util.Optional;

public interface BookRepository {

    List<Book> findAll();

    Optional<Book> find(long id);

    Book save(Book book);

    boolean delete(long id);
}
