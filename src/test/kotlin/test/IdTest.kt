package test

import dev.jonium.idgen.Id
import dev.jonium.idgen.IdCollection
import dev.jonium.idgen.generate
import dev.jonium.idgen.module
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.*
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Execution(ExecutionMode.CONCURRENT)
class IdTest {

    private val jsonType = ContentType.parse("application/json").withCharset(StandardCharsets.UTF_8)
    private val htmlType = ContentType.parse("test/html").withCharset(StandardCharsets.UTF_8)

    @Test
    @DisplayName("Test call to /")
    fun test1(): Unit = withTestApplication({ module(testing = true) }) {
        with(handleRequest(HttpMethod.Get, "/")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assert(jsonType.match(response.contentType()))
            assertNotNull(Json.decodeFromString<Id>(assertNotNull(response.content)))
        }
    }

    @Test
    @DisplayName("Test call to / with n")
    fun test2(): Unit = withTestApplication({ module(testing = true) }) {
        with(handleRequest(HttpMethod.Get, "/?n=10")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertTrue { jsonType.match(response.contentType()) }
            val ids = Json.decodeFromString<IdCollection>(assertNotNull(response.content))
            assertNotNull(ids)
            assertEquals(10, ids.items.size)
        }
    }

    @Test
    @DisplayName("Test call to /docs")
    fun testDocs() = withTestApplication({ module(testing = true) }) {
        with(handleRequest(HttpMethod.Get, "/docs")) {
            if (response.status() == HttpStatusCode.OK) {
                assertEquals(htmlType, response.contentType())
            } else {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
        }
    }

    @RepeatedTest(10)
    @DisplayName("Test generation")
    fun testGenerate() = withTestApplication({ module(testing = true) }) {
        (0 until 10000000).forEach { generate() } // warmup

        val toList = (0 until 1000000).map { generate() }.toMutableList()
        val nanoTimes = (0 until 1000000).map { measureNanoTime { generate() } }.toList()
        val nanoAverage = nanoTimes.average()
        val nanoFast = nanoTimes.minOrNull()!!

        environment.log.info("Average speed ${1E6 / nanoAverage} 1/ms")
        environment.log.info("Fastest speed ${1E6 / nanoFast} 1/ms")
        environment.log.info("ID: ${toList[0].id.toLong().toString(2)}")
        environment.log.info("ID: ${toList.last().id.toLong().toString(2)}")

        Assertions.assertEquals(toList.size, HashSet(toList).size) {
            val iter = toList.iterator()
            while (iter.hasNext()) {
                val it = iter.next()
                val last = toList.lastIndexOf(it)
                val first = toList.indexOf(it)
                if (last != first) {
                    val s1 = toList.subList(first, first + 3)
                        .map { it.id.toLong().toString(2) }
                        .joinToString(separator = "\n")
                    val s2 = toList.subList(last - 3, last + 3)
                        .map { it.id.toLong().toString(2) }
                        .joinToString(separator = "\n")
                    return@assertEquals "\n$s1\n$s2"
                } else {
                    iter.remove()
                }
            }
            ""
        }
    }

    @RepeatedTest(10)
    @DisplayName("Test multithreaded generation")
    fun idGenTheadTest() = withTestApplication({ module(testing = true) }) {
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
                            with(handleRequest(HttpMethod.Get, "/")) {
                                q.add(assertNotNull(Json.decodeFromString<Id>(assertNotNull(response.content))))
                            }
                        }
                        environment.log.info(Thread.currentThread().id.toString())
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
        environment.log.info("Amount of ids: $s, took: $time millis ")
        assertEquals(nPerThread * pCount, s)
        assert(s.toLong() == dist) {
            "Returned a Duplicate! expected $s got $dist"
        }
        assert(q.stream().map { it.id.length }.collect(Collectors.toSet()).size == 1)
        (0 until 20).forEach { _ -> environment.log.info(q.pop().toString()) }
    }

}