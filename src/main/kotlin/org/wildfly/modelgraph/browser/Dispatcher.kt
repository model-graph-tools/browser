package org.wildfly.modelgraph.browser

import dev.fritz2.routing.encodeURIComponent
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit

const val CAPABILITY_BASE = "https://raw.githubusercontent.com/wildfly/wildfly-capabilities/master"
const val ENDPOINT = "/mgtapi"

val json = Json {
    classDiscriminator = "modelType"
    ignoreUnknownKeys = true
}

val jsonPrettyPrint = Json {
    prettyPrint = true
}

class Dispatcher(private val registry: Registry) {

    suspend fun children(address: String): List<Resource> = failSafeRegistry(emptyList()) {
        json.decodeFromString(
            window.fetch(
                buildString {
                    append(ENDPOINT)
                    append("/resources/children/")
                    append(registry.current.selection.firstOrNull()?.identifier)
                    append("?address=")
                    append(encodeURIComponent(address))
                }
            ).await().text().await()
        )
    }

    suspend fun deprecated(since: String? = null): Models = failSafeRegistry(Models()) {
        json.decodeFromString(window.fetch(
            buildString {
                append(ENDPOINT)
                append("/management-model/deprecated/")
                append(registry.current.selection.firstOrNull()?.identifier)
                since?.let {
                    append("?since=")
                    append(since)
                }
            }
        ).await().text().await())
    }

    suspend fun diff(address: String, from: String, to: String): JsonArray = failSafeFetch(JsonArray(emptyList())) {
        json.parseToJsonElement(
            window.fetch(
                buildString {
                    append(ENDPOINT)
                    append("/diff?address=")
                    append(encodeURIComponent(address))
                    append("&from=")
                    append(encodeURIComponent(from))
                    append("&to=")
                    append(encodeURIComponent(to))
                }
            ).await().text().await()
        ).jsonArray
    }

    suspend fun query(name: String): Models = failSafeRegistry(Models()) {
        json.decodeFromString(
            window.fetch(
                buildString {
                    append(ENDPOINT)
                    append("/management-model/query/")
                    append(registry.current.selection.firstOrNull()?.identifier)
                    append("?name=")
                    append(encodeURIComponent(name))
                }
            ).await().text().await()
        )
    }

    suspend fun registry(): List<Registration> = failSafeFetch(emptyList()) {
        json.decodeFromString(
            window.fetch("$ENDPOINT/registry").await().text().await()
        )
    }

    suspend fun resource(address: String): Resource = failSafeRegistry(Resource.UNDEFINED) {
        json.decodeFromString<Resource>(
            window.fetch(
                buildString {
                    append(ENDPOINT)
                    append("/resources/resource/")
                    append(registry.current.selection.firstOrNull()?.identifier)
                    append("?address=")
                    append(encodeURIComponent(address))
                    if (Operation.globalOperations.isNotEmpty()) {
                        append("&skip=g")
                    }
                }
            ).await().text().await()
        ).also { resource ->
            if (Operation.globalOperations.isEmpty() && resource.operations.any { it.global }) {
                Operation.globalOperations = resource.operations.filter { it.global }.toList()
            }
        }.let { resource ->
            if (Operation.globalOperations.isNotEmpty() && resource.operations.none { it.global }) {
                resource.copy(operations = resource.operations + Operation.globalOperations)
            } else {
                resource
            }
        }
    }

    suspend fun resourceForDiff(identifier: String, address: String): JsonObject =
        failSafeFetch(JsonObject(emptyMap())) {
            val requestInit = RequestInit().apply {
                headers = Headers().apply { append("mgt-diff", "true") }
            }
            json.parseToJsonElement(
                window.fetch(
                    buildString {
                        append(ENDPOINT)
                        append("/resources/resource/")
                        append(identifier)
                        append("?address=")
                        append(encodeURIComponent(address))
                    },
                    requestInit
                ).await().text().await()
            ).jsonObject
        }

    suspend fun subtree(address: String): Resource = failSafeRegistry(Resource.UNDEFINED) {
        json.decodeFromString(
            window.fetch(
                buildString {
                    append(ENDPOINT)
                    append("/resources/subtree/")
                    append(registry.current.selection.firstOrNull()?.identifier)
                    append("?address=")
                    append(encodeURIComponent(address))
                }
            ).await().text().await()
        )
    }

    suspend fun versions(): List<Version> = failSafeRegistry(emptyList()) {
        json.decodeFromString(
            window.fetch(
                buildString {
                    append(ENDPOINT)
                    append("/versions/")
                    append(registry.current.selection.firstOrNull()?.identifier)
                }
            ).await().text().await()
        )
    }

    private suspend fun <T> failSafeFetch(defaultValue: T, block: suspend () -> T): T = try {
        block()
    } catch (t: Throwable) {
        console.log("Failed to call api service: ${t.message ?: "n/a"}")
        defaultValue
    }

    private suspend fun <T> failSafeRegistry(defaultValue: T, block: suspend () -> T): T = try {
        if (registry.current.all.isEmpty()) {
            console.log("No WildFly version available!")
            defaultValue
        } else {
            block()
        }
    } catch (t: Throwable) {
        console.log("Failed to call api service: ${t.message ?: "n/a"}")
        defaultValue
    }
}
