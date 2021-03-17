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
    fun getId(): Response {
        return Response.ok().entity(
            """
            {
              "openapi": "3.0.1",
              "info": {
                "title": "IDGen",
                "version": "1.0.0",
                "description": "An ID Generator inspired by Discord and Twitter snowflake formats",
                "contact": {
                  "name": "GitHub Repo",
                  "url": "https://github.com/joniumGit/IDGen"
                },
                "license": {
                  "name": "Apache 2.0",
                  "url": "https://www.apache.org/licenses/LICENSE-2.0.html"
                }
              },
              "paths": {
                "/": {
                  "get": {
                    "tags": [
                      "Api"
                    ],
                    "summary": "ApiDoc",
                    "responses": {
                      "200": {
                        "description": "Returns ApiDoc",
                        "content": {
                          "application/json": {}
                        }
                      }
                    }
                  }
                },
                "/id": {
                  "get": {
                    "tags": [
                      "Api"
                    ],
                    "summary": "Generates an ID per request",
                    "responses": {
                      "200": {
                        "description": "An ID",
                        "content": {
                          "application/json": {
                            "schema": {
                              "type": "object",
                              "properties": {
                                "id": {
                                  "type": "string",
                                  "minLength": 7,
                                  "maxLength": 20,
                                  "nullable": false,
                                  "pattern": "^\\d{7,20}            ${'$'}            "
                                }
                              },
                              "required": [
                                "id"
                              ]
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """.trimIndent()
        ).build()
    }
}

