package dev.jonium.idgen

import jakarta.ws.rs.ApplicationPath
import org.glassfish.jersey.server.ResourceConfig

@ApplicationPath("/")
class IdApplication : ResourceConfig() {

    init {
        packages("dev.jonium.idgen")
    }

}