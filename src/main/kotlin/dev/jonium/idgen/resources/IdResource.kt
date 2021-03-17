package dev.jonium.idgen.resources

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import dev.jonium.idgen.logic.id
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/id")
class IdResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getId(): Response {
        return try {
            Response.ok()
                .type(MediaType.APPLICATION_JSON)
                .entity(ObjectMapper().writeValueAsString(id()))
                .build()
        } catch (e: JsonProcessingException) {
            Response.serverError()
                .type(MediaType.APPLICATION_JSON)
                .entity("""{"message":${e.message}}""")
                .build()
        }
    }
}