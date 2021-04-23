package org.wildfly.modelgraph.browser

import kotlinx.serialization.Serializable

@Serializable
data class Registration(
    val identifier: String,
    val productName: String,
    val productVersion: String,
    val managementVersion: String,
    val modelServiceUri: String,
    val neo4jBrowserUri: String
) {
    override fun toString(): String = "$productName $productVersion"
}
