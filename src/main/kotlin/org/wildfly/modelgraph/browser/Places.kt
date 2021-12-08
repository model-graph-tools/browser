package org.wildfly.modelgraph.browser

import org.patternfly.mvp.PlaceRequest
import org.patternfly.mvp.placeRequest

const val BROWSE = "browse"
const val HOME = "home"
const val QUERY = "query"
const val DEPRECATION = "deprecation"
const val DIFF = "diff"
const val NEO4J = "neo4j"

fun browseAttribute(address: String, attribute: String): PlaceRequest =
    placeRequest(BROWSE, Pair("address", address), Pair("attribute", attribute))

fun browseCapability(capability: String): PlaceRequest =
    placeRequest(BROWSE, Pair("capability", capability))

fun browseOperation(address: String, operation: String): PlaceRequest =
    placeRequest(BROWSE, Pair("address", address), Pair("operation", operation))

fun browseResource(address: String): PlaceRequest =
    placeRequest(BROWSE, Pair("address", address))
