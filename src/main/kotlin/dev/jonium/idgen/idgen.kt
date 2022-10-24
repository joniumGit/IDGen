package dev.jonium.idgen

import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicLong


private val threadCounter = AtomicLong(0)
private val threadId: ThreadLocal<Long> = ThreadLocal.withInitial { threadCounter.getAndIncrement() }
private val rolling: ThreadLocal<Long> = ThreadLocal.withInitial { 0 }
private val lastTime: ThreadLocal<Long> = ThreadLocal.withInitial { 0 }

@Serializable
data class Id(val id: String)

@Serializable
data class IdCollection(val items: List<String>)

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