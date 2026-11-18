package edu.campus.library.repository;

import edu.campus.library.domain.Book;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class InMemoryBookRepository implements BookRepository {

    private final Map<Long, Book> books = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @Override
    public List<Book> findAll() {
        return new ArrayList<>(books.values());
    }

    @Override
    public Optional<Book> find(long id) {
        return Optional.ofNullable(books.get(id));
    }

    @Override
    public Book save(Book book) {
        Book stored = book.id() == null
                ? new Book(nextId.getAndIncrement(), book.title(), book.author(), book.isbn(), book.available())
                : book;
        books.put(stored.id(), stored);
        return stored;
    }

    @Override
    public boolean delete(long id) {
        return books.remove(id) != null;
    }
}
