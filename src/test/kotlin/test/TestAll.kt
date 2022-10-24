package test

import dev.jonium.idgen.IDGenServlet
import dev.jonium.idgen.Id
import dev.jonium.idgen.IdCollection
import dev.jonium.idgen.generate
import jakarta.servlet.Servlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertDoesNotThrow
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Function
import java.util.stream.Collectors
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis
import kotlin.test.*

class TestServer {
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

    private fun getId() = ByteArrayOutputStream().use { bao ->
        PrintWriter(bao).use { pw ->
            server.service(
                req(
                    mapOf(
                        "getMethod" to Function { "GET" },
                        "getPathInfo" to Function { "/" },
                        "getParameter" to Function {
                            assertEquals("n", it[0])
                            null
                        }
                    )
                ),
                resp(
                    mapOf(
                        "setContentType" to Function { args ->
                            assertEquals("application/json", args[0])
                        },
                        "setStatus" to Function { args ->
                            assertEquals(200, args[0])
                        },
                        "getWriter" to Function { pw },
                    )
                )
            )
            assertDoesNotThrow { Json.decodeFromString<Id>(bao.toString()) }
        }
    }

    @Test
    fun testHTTPGenerate() {
        assertNotNull(getId())
    }

    @Test
    fun testHTTPGenerateMultiple() {
        ByteArrayOutputStream().use { bao ->
            PrintWriter(bao).use { pw ->
                server.service(
                    req(
                        mapOf(
                            "getMethod" to Function { "GET" },
                            "getPathInfo" to Function { "/" },
                            "getParameter" to Function { args ->
                                assertEquals("n", args[0])
                                "10"
                            }
                        )
                    ),
                    resp(
                        mapOf(
                            "setContentType" to Function { args ->
                                assertEquals("application/json", args[0])
                            },
                            "setStatus" to Function { args ->
                                assertEquals(200, args[0])
                            },
                            "getWriter" to Function { pw },
                        )
                    )
                )
            }
            assertEquals(
                10,
                Json.decodeFromString<IdCollection>(bao.toString()).items.size
            )
        }
    }

    @Test
    fun testHTTPDocs() {
        ByteArrayOutputStream().use { bao ->
            PrintWriter(bao).use { pw ->
                server.service(
                    req(
                        mapOf(
                            "getMethod" to Function { "GET" },
                            "getPathInfo" to Function { "/docs" },
                        )
                    ),
                    resp(
                        mapOf(
                            "setContentType" to Function { args ->
                                assertEquals("text/text", args[0])
                            },
                            "setStatus" to Function { args ->
                                assertEquals(200, args[0])
                            },
                            "getWriter" to Function { pw },
                        )
                    )
                )
            }
            assertNotEquals(0, bao.size())
        }
    }

    @Test
    fun testGenerate() {
        (0 until 10000000).forEach { _ -> generate() } // warmup

        val toList = (0 until 1000000).map { generate() }.toMutableList()
        val nanoTimes = (0 until 1000000).map { measureNanoTime { generate() } }.toList()
        val nanoAverage = nanoTimes.average()
        val nanoFast = nanoTimes.minOrNull()!!

        println("Average speed ${1E6 / nanoAverage} 1/ms")
        println("Fastest speed ${1E6 / nanoFast} 1/ms")
        println("ID: ${toList[0].id.toLong().toString(2)}")
        println("ID: ${toList.last().id.toLong().toString(2)}")

        Assertions.assertEquals(toList.size, HashSet(toList).size) {
            val iter = toList.iterator()
            while (iter.hasNext()) {
                val it = iter.next()
                val last = toList.lastIndexOf(it)
                val first = toList.indexOf(it)
                if (last != first) {
                    val s1 = toList.subList(first, first + 3).joinToString(separator = "\n") {
                        it.id.toLong().toString(2)
                    }
                    val s2 = toList.subList(last - 3, last + 3).joinToString(separator = "\n") {
                        it.id.toLong().toString(2)
                    }
                    return@assertEquals "\n$s1\n$s2"
                } else {
                    iter.remove()
                }
            }
            ""
        }
    }

    @Test
    fun idGenerateThreaded() {
        val pCount = Runtime.getRuntime().availableProcessors() * 4
        val nPerThread = 1000

        val q = ConcurrentLinkedDeque<Id>()
        val e = Executors.newFixedThreadPool(pCount / 2)
        val l = CountDownLatch(1)

        val time = measureTimeMillis {
            try {
                for (i in 0 until pCount) {
                    e.submit {
                        try {
                            l.await()
                        } catch (ignore: InterruptedException) {
                        }
                        for (i1 in 0 until nPerThread) {
                            q.add(getId())
                        }
                    }
                }
            } finally {
                l.countDown()
                e.shutdown()
            }
            assertDoesNotThrow { assert(e.awaitTermination(10, TimeUnit.SECONDS)) }
        }

        val s = q.size
        val dist = q.stream().distinct().count()

        println("Amount of ids: $s, took: $time millis ")

        assertEquals(nPerThread * pCount, s)
        assert(s.toLong() == dist) { "Returned a Duplicate! expected $s got $dist" }
        assert(q.stream().map { it.id.length }.collect(Collectors.toSet()).size == 1)
    }

}