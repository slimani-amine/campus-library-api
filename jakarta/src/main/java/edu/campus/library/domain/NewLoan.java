package edu.campus.library.domain;

import jakarta.validation.constraints.NotNull;

public record NewLoan(@NotNull Long bookId, @NotNull Long memberId) {
}
