package edu.campus.library.api;

import edu.campus.library.domain.Loan;
import edu.campus.library.domain.NewLoan;
import edu.campus.library.service.LoanService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;

@Path("/loans")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class LoanResource {

    private LoanService service;

    /** Only for CDI: it needs a no-arg constructor to build the proxy of this bean. */
    protected LoanResource() {
    }

    @Inject
    public LoanResource(LoanService service) {
        this.service = service;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response borrow(@NotNull @Valid NewLoan request, @Context UriInfo uriInfo) {
        Loan loan = service.borrow(request.bookId(), request.memberId());
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(loan.id())).build();
        return Response.created(location).entity(loan).build();
    }

    @POST
    @Path("{id}/return")
    public Loan giveBack(@PathParam("id") long id) {
        return service.giveBack(id);
    }
}
