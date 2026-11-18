package edu.campus.library.repository;

import edu.campus.library.domain.Loan;

import java.util.List;
import java.util.Optional;

/** Where loans are stored. */
public interface LoanRepository {

    List<Loan> findAll();

    Optional<Loan> find(long id);

    /** Inserts the loan if its id is null (a new id is assigned), otherwise replaces it. */
    Loan save(Loan loan);
}
