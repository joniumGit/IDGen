package test

import dev.jonium.idgen.IDGenServlet
import dev.jonium.idgen.Id
import dev.jonium.idgen.IdCollection
import jakarta.servlet.Servlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.fail
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.function.Function
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

sealed class ServletTestSupport {

    private lateinit var server: Servlet

    private inline fun <reified T : Any> mock(
        data: Map<String, Function<Array<Any?>, Any?>>
    ) = Proxy.newProxyInstance(
        this.javaClass.classLoader,
        arrayOf(T::class.java),
        (InvocationHandler { _, method, args ->
            data[method.name]?.apply(args ?: emptyArray())
        })
    ) as T

    private fun req(data: Map<String, Function<Array<Any?>, Any?>>) = mock<HttpServletRequest>(data)

    private fun resp(data: Map<String, Function<Array<Any?>, Any?>>) = mock<HttpServletResponse>(data)


    @BeforeTest
    fun setup() {
        server = IDGenServlet()
    }

    @AfterTest
    fun teardown() {
        server.destroy()
    }

    protected fun get(
        path: String?,
        contentType: String = "text/text",
        status: Int = 200,
        requestExtra: Array<Pair<String, Function<Array<Any?>, Any?>>> = arrayOf(),
        responseExtra: Array<Pair<String, Function<Array<Any?>, Any?>>> = arrayOf(),
    ) = ByteArrayOutputStream().use { bao ->
        PrintWriter(bao).use { pw ->
            server.service(
                req(
                    mapOf(
                        "getMethod" to Function { "GET" },
                        "getPathInfo" to Function { path },
                        *requestExtra
                    )
                ),
                resp(
                    mapOf(
                        "setContentType" to Function { args ->
                            assertEquals(contentType, args[0])
                        },
                        "setStatus" to Function { args ->
                            assertEquals(status, args[0])
                        },
                        "getWriter" to Function { pw },
                        *responseExtra,
                    )
                ),
            )
        }
        bao.toString()
    }

    protected fun getId(status: Int, n: String? = null): String = get(
        "/",
        "application/json",
        status,
        requestExtra = arrayOf(
            "getParameter" to Function {
                assertEquals("n", it[0])
                n
            }
        ),
    )

    protected fun getId() = getId(200).let {
        runCatching {
            Json.decodeFromString<Id>(it)
        }.getOrElse {
            fail(it)
        }
    }

    protected fun getIds(n: Int) = getId(200, n.toString(10)).let {
        runCatching {
            Json.decodeFromString<IdCollection>(it)
        }.getOrElse {
            fail(it)
        }
    }

}