@file:JvmName("BigIntStringifier")

package dev.jonium.idgen.logic

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import java.math.BigInteger

class BigIntStringifier : JsonSerializer<BigInteger>() {

    override fun serialize(
        bigInt: BigInteger,
        jgen: JsonGenerator,
        p2: SerializerProvider?
    ) {
        jgen.writeString(bigInt.toString(10))
    }
}