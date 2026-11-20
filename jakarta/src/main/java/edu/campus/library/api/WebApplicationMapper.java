package edu.campus.library.api;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Errors raised by the REST runtime itself, before our code runs: unknown path or
 * non-numeric id (404), wrong method (405), wrong Content-Type (415), wrong Accept (406).
 * Same problem+json body as our own errors; the Allow header of a 405 is kept.
 */
@Provider
public class WebApplicationMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        Response original = exception.getResponse();
        Response.StatusType status = original.getStatusInfo();
        Response problem = Problems.response(status, status.getReasonPhrase());
        String allow = original.getHeaderString(HttpHeaders.ALLOW);
        if (allow == null) {
            return problem;
        }
        return Response.fromResponse(problem).header(HttpHeaders.ALLOW, allow).build();
    }
}
