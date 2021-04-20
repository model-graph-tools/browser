package org.wildfly.modelgraph.browser

import dev.fritz2.remote.http
import dev.fritz2.routing.encodeURIComponent
import kotlinx.coroutines.await
import kotlinx.serialization.decodeFromString

const val CAPABILITY_BASE = "https://raw.githubusercontent.com/wildfly/wildfly-capabilities/master"

class Dispatcher(private val registry: Registry) {

    private val endpoint: String = Environment.failSafe(process.env.MGT_API) { "http://localhost:9911" }

    suspend fun registry(): List<Registration> = json.decodeFromString(
        http("$endpoint/registry")
            .acceptJson()
            .get()
            .text()
            .await())

    suspend fun children(address: String): List<Resource> = json.decodeFromString(
        http("$endpoint/resources/children/${registry.active.identifier}?address=${encodeURIComponent(address)}")
            .acceptJson()
            .get()
            .text()
            .await()
    )

    suspend fun resource(address: String): Resource {
        val endpoint = buildString {
            append(endpoint)
            append("/resources/resource/")
            append(registry.active.identifier)
            append("?address=")
            append(encodeURIComponent(address))
            if (Operation.globalOperations.isNotEmpty()) {
                append("&skip=g")
            }
        }
        return json.decodeFromString<Resource>(
            http(endpoint)
                .acceptJson()
                .get()
                .text()
                .await()
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

    suspend fun subtree(address: String): Resource = json.decodeFromString(
        http("$endpoint/resources/subtree/${registry.active.identifier}?address=${encodeURIComponent(address)}")
            .acceptJson()
            .get()
            .text()
            .await()
    )

    suspend fun query(name: String): Models = json.decodeFromString(
        http("$endpoint/management-model/query/${registry.active.identifier}?name=${encodeURIComponent(name)}")
            .acceptJson()
            .get()
            .text()
            .await()
    )
}
