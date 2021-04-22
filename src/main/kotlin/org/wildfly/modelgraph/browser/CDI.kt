package org.wildfly.modelgraph.browser

import dev.fritz2.mvp.PlaceManager
import dev.fritz2.mvp.placeRequest
import org.patternfly.pageSection
import org.patternfly.title

fun cdi(): CDI = CDIInstance

interface CDI {
    val placeManager: PlaceManager
    val activeRegistration: ActiveRegistration
    val registry: Registry
    val dispatcher: Dispatcher
}

internal object CDIInstance : CDI {

    override val placeManager = PlaceManager(placeRequest(HOME)) { placeRequest ->
        pageSection {
            title { +"Not Found" }
            p {
                +"Page "
                code { +placeRequest.token }
                +" not found"
            }
        }
    }

    override val activeRegistration: ActiveRegistration = ActiveRegistration()

    override val registry: Registry = Registry()

    override val dispatcher: Dispatcher = Dispatcher(activeRegistration)
}
