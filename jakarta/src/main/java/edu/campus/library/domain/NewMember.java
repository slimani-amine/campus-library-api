package edu.campus.library.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Request body for creating a member. */
public record NewMember(@NotBlank String name, @NotBlank @Email String email) {
}
