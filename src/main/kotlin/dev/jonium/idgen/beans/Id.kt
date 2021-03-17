package dev.jonium.idgen.beans

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import dev.jonium.idgen.logic.BigIntStringifier
import java.math.BigInteger

data class Id(@JsonProperty("id") @JsonSerialize(using = BigIntStringifier::class) val id: BigInteger)
