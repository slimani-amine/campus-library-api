package edu.campus.library.api;

import edu.campus.library.service.ConflictException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Business rule broken (book already lent, member has 3 loans, loan already returned) → 409. */
@Provider
public class ConflictMapper implements ExceptionMapper<ConflictException> {

    @Override
    public Response toResponse(ConflictException exception) {
        return Problems.response(Response.Status.CONFLICT, exception.getMessage());
    }
}
