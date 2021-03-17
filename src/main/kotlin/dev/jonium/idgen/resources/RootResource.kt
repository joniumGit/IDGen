package dev.jonium.idgen.resources

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/")
class RootResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getId(): Response = Response.ok().build()
}