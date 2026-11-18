package edu.campus.library.domain;

import jakarta.json.bind.annotation.JsonbNillable;

import java.time.Instant;

/**
 * A book lent to a member. {@code returnedAt} is null while the loan is active.
 * {@code @JsonbNillable} makes JSON-B write {@code "returnedAt": null} instead of
 * leaving the field out, so the JSON always has the five fields of the contract.
 */
@JsonbNillable
public record Loan(Long id, long bookId, long memberId, Instant loanedAt, Instant returnedAt) {

    public boolean active() {
        return returnedAt == null;
    }
}
