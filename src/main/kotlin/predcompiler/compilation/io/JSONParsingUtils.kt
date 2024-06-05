package predcompiler.compilation.io

import kotlinx.serialization.json.*
import predcompiler.compilation.AbstractPredicateSearch
import java.io.File
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

val json = Json { ignoreUnknownKeys = true }

fun <T> loadClassFromJSON(jsonObject: JsonObject): T {

    val className = jsonObject["class"]?.jsonPrimitive?.content
        ?: throw IllegalArgumentException("Missing 'class' field in JSON")
    val data = jsonObject["data"]?.jsonObject
        ?: throw IllegalArgumentException("Missing 'data' field in JSON")

    val kClass = Class.forName(className).kotlin
    val constructor = kClass.primaryConstructor!!
    val args = constructor.parameters.associateWith { parameter ->
        val value = data[parameter.name]
        when {
            value == null -> null
            parameter.type.jvmErasure == String::class -> value.jsonPrimitive.content
            parameter.type.jvmErasure == Int::class -> value.jsonPrimitive.int
            parameter.type.jvmErasure == Float::class -> value.jsonPrimitive.float
            parameter.type.jvmErasure == Double::class -> value.jsonPrimitive.double
            parameter.type.jvmErasure == Boolean::class -> value.jsonPrimitive.boolean
            parameter.type.jvmErasure == List::class -> {
                value.jsonArray.map { loadClassFromJSON<Any>(it.jsonObject) }
            }
            else -> loadClassFromJSON<Any>(value.jsonObject)
        }
    }
    // TODO consistent type check
    return constructor.callBy(args) as T
}

fun <T> loadClassFromFile(filePath: String): T {
    val fileContent = File(filePath).readText()
    val jsonObject = json.parseToJsonElement(fileContent).jsonObject

    return loadClassFromJSON(jsonObject)
}

fun main() {
    val filePath = "src/main/resources/experiments/ping_pong_mcts_sample.json"
    val res = loadClassFromFile<AbstractPredicateSearch>(filePath)
    println(res)
}
