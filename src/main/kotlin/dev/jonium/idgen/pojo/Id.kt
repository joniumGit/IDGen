package dev.jonium.idgen.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigInteger

data class Id(@JsonProperty("id") val id: BigInteger)
