package test

import dev.jonium.idgen.Id
import dev.jonium.idgen.generate
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals

class TestGeneration {


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
                            q.add(generate())
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