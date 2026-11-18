package edu.campus.library.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record NewMember(@NotBlank String name, @NotBlank @Email String email) {
}
