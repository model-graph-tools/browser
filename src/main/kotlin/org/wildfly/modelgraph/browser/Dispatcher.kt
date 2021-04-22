package org.wildfly.modelgraph.browser

import dev.fritz2.routing.encodeURIComponent
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.decodeFromString

const val CAPABILITY_BASE = "https://raw.githubusercontent.com/wildfly/wildfly-capabilities/master"
const val ENDPOINT = "/mgtapi"

class Dispatcher(private val registration: ActiveRegistration) {

    suspend fun registry(): List<Registration> = json.decodeFromString(
        window.fetch("$ENDPOINT/registry").await().text().await()
    )

    suspend fun children(address: String): List<Resource> = json.decodeFromString(
        window.fetch(
            buildString {
                append(ENDPOINT)
                append("/resources/children/")
                append(registration.current.identifier)
                append("?address=")
                append(encodeURIComponent(address))
            }
        ).await().text().await()
    )

    suspend fun resource(address: String): Resource = json.decodeFromString<Resource>(
        window.fetch(
            buildString {
                append(ENDPOINT)
                append("/resources/resource/")
                append(registration.current.identifier)
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

    suspend fun subtree(address: String): Resource = json.decodeFromString(
        window.fetch(
            buildString {
                append(ENDPOINT)
                append("/resources/subtree/")
                append(registration.current.identifier)
                append("?address=")
                append(encodeURIComponent(address))
            }
        ).await().text().await()
    )

    suspend fun query(name: String): Models = json.decodeFromString(
        window.fetch(
            buildString {
                append(ENDPOINT)
                append("/management-model/query/")
                append(registration.current.identifier)
                append("?name=")
                append(encodeURIComponent(name))
            }
        ).await().text().await()
    )
}
