package edu.campus.library.api;

import java.util.List;

/**
 * RFC 9457 error body, sent as application/problem+json.
 * {@code errors} is only filled for validation failures; when it is null JSON-B leaves it out.
 */
public record ProblemDetail(String type, String title, int status, String detail, List<FieldError> errors) {

    public static final String MEDIA_TYPE = "application/problem+json";

    /** One invalid field in a validation failure. */
    public record FieldError(String field, String message) {
    }
}
