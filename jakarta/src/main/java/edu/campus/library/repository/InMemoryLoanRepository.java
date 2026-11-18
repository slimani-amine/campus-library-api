package edu.campus.library.repository;

import edu.campus.library.domain.Loan;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class InMemoryLoanRepository implements LoanRepository {

    private final Map<Long, Loan> loans = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @Override
    public List<Loan> findAll() {
        return new ArrayList<>(loans.values());
    }

    @Override
    public Optional<Loan> find(long id) {
        return Optional.ofNullable(loans.get(id));
    }

    @Override
    public Loan save(Loan loan) {
        Loan stored = loan.id() == null
                ? new Loan(nextId.getAndIncrement(), loan.bookId(), loan.memberId(), loan.loanedAt(), loan.returnedAt())
                : loan;
        loans.put(stored.id(), stored);
        return stored;
    }
}
