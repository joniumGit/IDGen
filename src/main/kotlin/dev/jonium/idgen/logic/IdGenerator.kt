package dev.jonium.idgen.logic

import dev.jonium.idgen.pojo.Id
import java.math.BigInteger
import java.util.*

private val sequence: ThreadLocal<Short> = ThreadLocal.withInitial { 0 }
private fun ThreadLocal<Short>.fetch(): Short {
    val i = this.get()
    if (i == 999.toShort()) {
        this.set(0)
    } else {
        this.set((i + 1).toShort())
    }
    return i
}

private val start by lazy {
    val c = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    c.set(Calendar.YEAR, 2020)
    c.set(Calendar.DAY_OF_MONTH, 1)
    c.set(Calendar.MONTH, 0)
    c.set(Calendar.HOUR_OF_DAY, 12)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.MILLISECOND, 0)
    c.toInstant().toEpochMilli()
}

private fun idFromTime(): String = String.format("%1$014d", System.currentTimeMillis() - start)
private fun idFromSequence(): String = String.format("%1$03d", sequence.fetch())
private fun idFromWorker(): String = String.format("%1$03d", Thread.currentThread().id)

fun id(): Id = Id(BigInteger(idFromTime() + idFromSequence() + idFromWorker()))