package edu.campus.library.api;

import edu.campus.library.domain.Member;
import edu.campus.library.domain.NewMember;
import edu.campus.library.service.MemberService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.List;

@Path("/members")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class MemberResource {

    private MemberService service;

    /** Only for CDI: it needs a no-arg constructor to build the proxy of this bean. */
    protected MemberResource() {
    }

    @Inject
    public MemberResource(MemberService service) {
        this.service = service;
    }

    @GET
    public List<Member> list() {
        return service.findAll();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@NotNull @Valid NewMember member, @Context UriInfo uriInfo) {
        Member created = service.create(member);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(created.id())).build();
        return Response.created(location).entity(created).build();
    }
}
