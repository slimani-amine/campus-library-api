package edu.campus.library.api;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.StatusType;

import java.util.List;
import java.util.Map;

/** The one place that builds problem+json responses, so all mappers look the same. */
final class Problems {

    private static final Map<Integer, String> TYPES = Map.of(
            400, "https://tools.ietf.org/html/rfc9110#section-15.5.1",
            404, "https://tools.ietf.org/html/rfc9110#section-15.5.5",
            405, "https://tools.ietf.org/html/rfc9110#section-15.5.6",
            406, "https://tools.ietf.org/html/rfc9110#section-15.5.7",
            409, "https://tools.ietf.org/html/rfc9110#section-15.5.10",
            415, "https://tools.ietf.org/html/rfc9110#section-15.5.16");

    private Problems() {
    }

    static Response response(StatusType status, String detail) {
        return response(status, detail, null);
    }

    static Response response(StatusType status, String detail, List<ProblemDetail.FieldError> errors) {
        int code = status.getStatusCode();
        ProblemDetail body = new ProblemDetail(
                TYPES.getOrDefault(code, "about:blank"), status.getReasonPhrase(), code, detail, errors);
        return Response.status(status).type(ProblemDetail.MEDIA_TYPE).entity(body).build();
    }
}
