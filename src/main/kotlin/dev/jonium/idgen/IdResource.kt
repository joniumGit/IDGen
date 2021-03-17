package dev.jonium.idgen

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import dev.jonium.idgen.logic.id
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

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