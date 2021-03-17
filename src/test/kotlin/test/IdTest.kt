package test

import dev.jonium.idgen.beans.Id
import dev.jonium.idgen.resources.IdResource
import dev.jonium.idgen.resources.RootResource
import jakarta.ws.rs.client.WebTarget
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.glassfish.jersey.logging.LoggingFeature
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.servlet.ServletContainer
import org.glassfish.jersey.test.DeploymentContext
import org.glassfish.jersey.test.JerseyTest
import org.glassfish.jersey.test.ServletDeploymentContext
import org.glassfish.jersey.test.TestProperties
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory
import org.glassfish.jersey.test.spi.TestContainerFactory
import org.junit.jupiter.api.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Level

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdTest {

    private lateinit var test: JerseyTest

    private inline fun <T> get(block: (WebTarget) -> T): T = block(test.target("id"))
    private inline fun getId(block: (Id) -> Unit) = block(test.target("id").request().get(Id::class.java))


    @BeforeAll
    fun before() {
        test = object : JerseyTest() {
            override fun configure(): ResourceConfig {
                enable(TestProperties.DUMP_ENTITY)
                enable(TestProperties.LOG_TRAFFIC)
                return ResourceConfig().property(
                    LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL_SERVER, Level.ALL.toString()
                ).registerClasses(IdResource::class.java, RootResource::class.java)
            }

            override fun getTestContainerFactory(): TestContainerFactory = GrizzlyWebTestContainerFactory()
            override fun configureDeployment(): DeploymentContext {
                return ServletDeploymentContext.forServlet(ServletContainer(configure())).build()
            }
        }
        test.setUp()
    }

    @AfterAll
    fun after() {
        test.tearDown()
    }

    @Test
    fun rootTest() {
        val resp = test.target("").request().get()
        assert(resp.status == Response.Status.OK.statusCode)
        assert(resp.mediaType == MediaType.APPLICATION_JSON_TYPE)
        println(resp.readEntity(String::class.java))
        println("Root ok")
    }

    @Test
    fun readTest() {
        get {
            val res = it.request().get()
            assert(res.status == Response.Status.OK.statusCode) {
                "Expected 200 got ${res.status} and ${res.readEntity(String::class.java)}"
            }
            assert(res.mediaType == MediaType.APPLICATION_JSON_TYPE) { "Expected JSON got ${res.mediaType}" }
            val id = res.readEntity(Id::class.java)
            println(id)
        }
    }

    @Test
    fun idGenTheadTest() {
        val q = ConcurrentLinkedDeque<Id>()
        val e = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2
        )
        val l = CountDownLatch(1)
        try {
            for (i in 0..Runtime.getRuntime().availableProcessors() * 4) {
                e.submit {
                    try {
                        l.await()
                    } catch (ignore: InterruptedException) {
                    }
                    for (i1 in 0..999) {
                        getId(q::add)
                    }
                }
            }
        } finally {
            l.countDown()
            e.shutdown()
        }
        assertDoesNotThrow { assert(e.awaitTermination(10, TimeUnit.SECONDS)) }
        val s = q.size
        val dist = q.stream().distinct().count()
        var i = 0
        q.stream().takeWhile {
            if (i < 20) {
                i += 1
                true
            } else {
                false
            }
        }.forEach(::println)
        assert(s.toLong() == dist) { "Returned a Duplicate! expected $s got $dist" }
    }

}