package edu.campus.library.service;

import edu.campus.library.domain.Book;
import edu.campus.library.domain.NewBook;
import edu.campus.library.repository.BookRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
@Logged
public class BookService {

    private BookRepository books;

    protected BookService() {
    }

    @Inject
    public BookService(BookRepository books) {
        this.books = books;
    }

    public List<Book> findAll() {
        return books.findAll();
    }

    public Book find(long id) {
        return books.find(id).orElseThrow(() -> new NotFoundException("Book " + id + " does not exist"));
    }

    public Book create(NewBook newBook) {
        return books.save(new Book(null, newBook.title(), newBook.author(), newBook.isbn(), true));
    }

    public Book update(long id, NewBook changes) {
        Book current = find(id);
        return books.save(new Book(id, changes.title(), changes.author(), changes.isbn(), current.available()));
    }

    public void delete(long id) {
        if (!books.delete(id)) {
            throw new NotFoundException("Book " + id + " does not exist");
        }
    }
}
