package edu.campus.library.api;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

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
