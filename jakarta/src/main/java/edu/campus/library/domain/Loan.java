package edu.campus.library.domain;

import jakarta.json.bind.annotation.JsonbNillable;

import java.time.Instant;

@JsonbNillable
public record Loan(Long id, long bookId, long memberId, Instant loanedAt, Instant returnedAt) {

    public boolean active() {
        return returnedAt == null;
    }
}
