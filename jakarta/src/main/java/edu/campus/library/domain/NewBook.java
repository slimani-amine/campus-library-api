package edu.campus.library.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NewBook(@NotBlank @Size(max = 200) String title, @NotBlank String author,
                      @Pattern(regexp = "\\d{13}") String isbn) {
}
