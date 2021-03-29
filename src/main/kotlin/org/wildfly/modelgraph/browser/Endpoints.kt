package org.wildfly.modelgraph.browser

import dev.fritz2.remote.http
import dev.fritz2.routing.encodeURIComponent
import kotlinx.coroutines.await
import kotlinx.serialization.decodeFromString

const val REST_API: String = "http://localhost:8080"
const val CAPABILITY_BASE = "https://raw.githubusercontent.com/wildfly/wildfly-capabilities/master"

object Endpoints {

    suspend fun children(address: String): List<Resource> = json.decodeFromString(
        http("$REST_API/resources/children?address=${encodeURIComponent(address)}")
            .acceptJson()
            .get()
            .text()
            .await()
    )

    suspend fun resource(address: String): Resource {
        val endpoint = buildString {
            append(REST_API)
            append("/resources/resource?address=")
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
        http("$REST_API/resources/subtree?address=${encodeURIComponent(address)}")
            .acceptJson()
            .get()
            .text()
            .await()
    )

    suspend fun query(name: String): Models = json.decodeFromString(
        http("$REST_API/management-model/query?name=${encodeURIComponent(name)}")
            .acceptJson()
            .get()
            .text()
            .await()
    )
}
