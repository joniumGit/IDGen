package dev.jonium.idgen

import jakarta.servlet.annotation.WebServlet
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


@WebServlet("/*")
class IDGenServlet : HttpServlet() {

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        when (
            req.pathInfo?.let {
                if (it.endsWith("/")) {
                    it.dropLast(1)
                } else {
                    it
                }
            } ?: ""
        ) {
            "" -> {
                resp.contentType = JSON
                resp.addHeader("Cache-Control", "no-store, no-cache, must-revalidate")
                resp.addHeader("Expires", "0")
                runCatching {
                    req.getParameter("n")?.let {
                        val n = it.toInt()
                        if (n > 1000 || n <= 0) {
                            throw IllegalArgumentException("Invalid id count")
                        } else {
                            Json.encodeToString(IdCollection((0 until n).map { generate().id }.toList()))
                        }
                    } ?: Json.encodeToString(generate())
                }.onFailure {
                    resp.status = 422
                    resp.contentType = JSON
                    resp.writer.use {
                        it.write("{\"error\": \"invalid parameters in request\"}")
                    }
                }.onSuccess { entity ->
                    resp.status = 200
                    resp.writer.use {
                        it.write(entity)
                    }
                }
            }

            "/docs" -> {
                resp.status = 200
                resp.contentType = TEXT
                resp.writer.use {
                    it.write(
                        """
                        IDGen:
                        
                        /docs:
                          returns this documentation page
                          
                        /:
                          parameters:
                            - n:
                              - range [1,1000]
                              - integer
                          examples:
                            - No n specified:
                            { "id": "239578532651466752" }
                            - n = 2:
                            { "items": [ "239578501894635520", "239578532651466752" ] }
                          returns a list of items (ids) if n is specified, otherwise a single id
                        
                        """.trimIndent()
                    )
                }
            }

            else -> {
                resp.status = 404
                resp.contentType = JSON
                resp.writer.use {
                    it.write("{\"error\": \"invalid path for request\"}")
                }
            }
        }
    }

}