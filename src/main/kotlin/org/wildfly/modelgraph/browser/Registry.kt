package org.wildfly.modelgraph.browser

import dev.fritz2.binding.EmittingHandler
import dev.fritz2.binding.RootStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import org.wildfly.modelgraph.browser.Registration.Companion.UNDEFINED

@Serializable
data class Registration(
    val identifier: String,
    val productName: String,
    val productVersion: String,
    val managementVersion: String,
    val modelServiceUri: String,
    val neo4jBrowserUri: String
) {
    companion object {
        val UNDEFINED: Registration = Registration(
            identifier = "-1",
            productName = "undefined",
            productVersion = "0.0.0",
            managementVersion = "0.0.0",
            modelServiceUri = "http://model.service.uri",
            neo4jBrowserUri = "http://neo4j.browser.uri"
        )
    }
}

class Registry : RootStore<List<Registration>>(listOf()) {
    val empty: Flow<Boolean> = data.map { it.isEmpty() }
}

class ActiveRegistration : RootStore<Registration>(UNDEFINED) {
    val undefined: Flow<Boolean> = data.map { it == UNDEFINED }
}
