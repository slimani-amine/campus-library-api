package edu.campus.library.service;

import edu.campus.library.domain.Book;
import edu.campus.library.domain.Loan;
import edu.campus.library.repository.BookRepository;
import edu.campus.library.repository.LoanRepository;
import edu.campus.library.repository.MemberRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Clock;
import java.time.Instant;

@ApplicationScoped
@Logged
public class LoanService {

    static final int MAX_ACTIVE_LOANS = 3;

    private BookRepository books;
    private MemberRepository members;
    private LoanRepository loans;
    private Clock clock;

    /** Only for CDI: it needs a no-arg constructor to build the proxy of this bean. */
    protected LoanService() {
    }

    @Inject
    public LoanService(BookRepository books, MemberRepository members, LoanRepository loans, Clock clock) {
        this.books = books;
        this.members = members;
        this.loans = loans;
        this.clock = clock;
    }

    public Loan borrow(long bookId, long memberId) {
        Book book = books.find(bookId)
                .orElseThrow(() -> new NotFoundException("Book " + bookId + " does not exist"));
        members.find(memberId)
                .orElseThrow(() -> new NotFoundException("Member " + memberId + " does not exist"));

        if (!book.available()) {
            throw new ConflictException("Book " + bookId + " is already on loan");
        }
        if (activeLoans(memberId) >= MAX_ACTIVE_LOANS) {
            throw new ConflictException("Member " + memberId + " already has " + MAX_ACTIVE_LOANS + " active loans");
        }

        // RACE (check-then-act): the two checks above and the two saves below are not atomic.
        // Two requests for the same book can both see "available" before either one saves,
        // so the book is lent twice (same for a member's 3-loan limit). Not fixed on purpose:
        // transactions come in Lab 3.
        books.save(new Book(book.id(), book.title(), book.author(), book.isbn(), false));
        return loans.save(new Loan(null, bookId, memberId, Instant.now(clock), null));
    }

    public Loan giveBack(long loanId) {
        Loan loan = loans.find(loanId)
                .orElseThrow(() -> new NotFoundException("Loan " + loanId + " does not exist"));
        if (!loan.active()) {
            throw new ConflictException("Loan " + loanId + " was already returned");
        }

        Loan returned = loans.save(new Loan(loan.id(), loan.bookId(), loan.memberId(), loan.loanedAt(), Instant.now(clock)));
        books.find(loan.bookId()).ifPresent(book ->
                books.save(new Book(book.id(), book.title(), book.author(), book.isbn(), true)));
        return returned;
    }

    private long activeLoans(long memberId) {
        return loans.findAll().stream()
                .filter(loan -> loan.memberId() == memberId && loan.active())
                .count();
    }
}
