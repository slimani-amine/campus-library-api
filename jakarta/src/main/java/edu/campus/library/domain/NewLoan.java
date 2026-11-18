package edu.campus.library.domain;

import jakarta.validation.constraints.NotNull;

/** Request body for borrowing a book. */
public record NewLoan(@NotNull Long bookId, @NotNull Long memberId) {
}
