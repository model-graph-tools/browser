package org.wildfly.modelgraph.browser

import dev.fritz2.mvp.PlaceManager
import dev.fritz2.mvp.placeRequest
import org.patternfly.ItemsStore
import org.patternfly.pageSection
import org.patternfly.title

fun cdi(): CDI = CDIInstance

interface CDI {
    val placeManager: PlaceManager
    val registry: ItemsStore<Registration>
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

    override val registry: ItemsStore<Registration> = ItemsStore<Registration> { it.identifier }

    override val dispatcher: Dispatcher = Dispatcher(registry)
}
