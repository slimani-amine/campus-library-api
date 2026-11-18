package edu.campus.library.api;

import edu.campus.library.domain.Book;
import edu.campus.library.domain.NewBook;
import edu.campus.library.service.BookService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.List;

@Path("/books")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class BookResource {

    private BookService service;

    /** Only for CDI: it needs a no-arg constructor to build the proxy of this bean. */
    protected BookResource() {
    }

    @Inject
    public BookResource(BookService service) {
        this.service = service;
    }

    @GET
    public List<Book> list() {
        return service.findAll();
    }

    @GET
    @Path("{id}")
    public Book get(@PathParam("id") long id) {
        return service.find(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@NotNull @Valid NewBook book, @Context UriInfo uriInfo) {
        Book created = service.create(book);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(created.id())).build();
        return Response.created(location).entity(created).build();
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Book update(@PathParam("id") long id, @NotNull @Valid NewBook book) {
        return service.update(id, book);
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") long id) {
        service.delete(id);
        return Response.noContent().build();
    }
}
