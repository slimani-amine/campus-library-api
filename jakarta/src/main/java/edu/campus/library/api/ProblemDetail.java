package edu.campus.library.api;

import java.util.List;

public record ProblemDetail(String type, String title, int status, String detail, List<FieldError> errors) {

    public static final String MEDIA_TYPE = "application/problem+json";

    public record FieldError(String field, String message) {
    }
}
