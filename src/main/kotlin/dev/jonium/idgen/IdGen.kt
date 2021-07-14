package dev.jonium.idgen

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicLong

/*
42 bits TIME - Approx 100 yrs.
5  bits THR  - Thread
4  bits RND  - Other
13 bits SEQ  - Sequence
 */

private const val EPOCH: Long = 1609459200000L

private const val SHL_RND: Int = 13
private const val SHL_THR: Int = 17
private const val SHL_TIME: Int = 22

private const val MASK_RND: Long = 7
private const val MASK_THR: Long = 31
private const val MASK_SEQ: Long = 8191

private const val MOD_THR: Long = MASK_THR + 1
private const val MOD_RND: Long = MASK_RND + 1
private const val MOD_SEQ: Long = MASK_SEQ + 1

private val threadCounter = AtomicLong(0)
private val threadId: ThreadLocal<Long> = ThreadLocal.withInitial { threadCounter.getAndIncrement() }
private val rolling: ThreadLocal<Long> = ThreadLocal.withInitial { 0 }
private val lastTime: ThreadLocal<Long> = ThreadLocal.withInitial { 0 }

@Serializable
data class Id(val id: String)

@Serializable
data class IdCollection(val items: List<String>)

@Serializable
data class Error(val message: String)

fun generate(): Id {
    var millis = System.currentTimeMillis().also {
        if (lastTime.get() - it != 0L) {
            rolling.set(0L)
            lastTime.set(it)
        }
    }

    val sequence = rolling.get().also { last ->
        (last + 1).mod(MOD_SEQ).let { next ->
            if (next == 0L) {
                var current = System.currentTimeMillis()
                while (millis - current == 0L) {
                    current = System.currentTimeMillis()
                }
                millis = current
                lastTime.set(current)
                rolling.set(0L)
            } else {
                rolling.set(next)
            }
        }
    } and MASK_SEQ

    val rnd = 0.mod(MOD_RND) and MASK_RND shl SHL_RND
    val thread = threadId.get().mod(MOD_THR) and MASK_THR shl SHL_THR
    val time = millis - EPOCH shl SHL_TIME

    return Id((time or rnd or thread or sequence).toString(10))
}

fun Route.docs() {
    static("/docs") {
        defaultResource("index.html")
    }
}

fun Route.ids() {
    get("/") {
        if ("n" in call.request.queryParameters) {
            call.request.queryParameters["n"]?.let {
                try {
                    val n = it.toLong()
                    if (n in 0..1000) {
                        call.respond(IdCollection((0 until it.toLong()).map { generate().id }.toList()))
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            Error("Value for n out of range: $n\nShould be 0 <= n <= 1000")
                        )
                    }
                } catch (e: NumberFormatException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        Error("Parameter n should be a number")
                    )
                }
            }
        } else {
            call.respond(generate())
        }
    }
}

fun main(args: Array<String>) = io.ktor.server.jetty.EngineMain.main(args)

fun Application.module(testing: Boolean = false) {
    environment.log.info("Testing: $testing")
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = false
        })
    }
    routing {
        docs()
        ids()
    }
}