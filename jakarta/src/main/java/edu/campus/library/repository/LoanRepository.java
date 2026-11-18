package edu.campus.library.repository;

import edu.campus.library.domain.Loan;

import java.util.List;
import java.util.Optional;

public interface LoanRepository {

    List<Loan> findAll();

    Optional<Loan> find(long id);

    Loan save(Loan loan);
}
