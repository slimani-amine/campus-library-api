package edu.campus.library.api;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Comparator;
import java.util.List;

/** Bean Validation failed on a request body (@Valid) → 400 with one entry per invalid field. */
@Provider
public class ValidationMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<ProblemDetail.FieldError> errors = exception.getConstraintViolations().stream()
                .map(v -> new ProblemDetail.FieldError(fieldName(v), v.getMessage()))
                .sorted(Comparator.comparing(ProblemDetail.FieldError::field))
                .toList();
        return Problems.response(Response.Status.BAD_REQUEST, "Validation failed", errors);
    }

    /**
     * "create.book.title" → "title": keep only the last part of the property path.
     * If the last part is the method parameter itself (the body is missing), say "body".
     */
    private static String fieldName(ConstraintViolation<?> violation) {
        String name = null;
        for (Path.Node node : violation.getPropertyPath()) {
            name = node.getKind() == ElementKind.PARAMETER ? "body" : node.getName();
        }
        return name;
    }
}
