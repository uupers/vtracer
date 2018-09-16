package none.nintha.vtraceapi.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

fun <T> JsonNode.asBean(valueType: Class<T>, mapper: ObjectMapper): T {
    return mapper.readValue(this.toString(), valueType)
}
