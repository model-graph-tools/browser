package org.wildfly.modelgraph.browser

import org.patternfly.mvp.PlaceManager
import org.patternfly.mvp.Presenter
import org.patternfly.mvp.placeRequest
import org.patternfly.ItemsStore
import org.patternfly.pageSection
import org.patternfly.title

fun cdi(): CDI = CDIInstance

interface CDI {
    val bootstrapTasks: List<() -> BootstrapTask>
    val placeManager: PlaceManager
    val registry: Registry
    val dispatcher: Dispatcher
}

internal object CDIInstance : CDI {

    init {
        Presenter.register(BROWSE) { BrowsePresenter(dispatcher, registry) }
        Presenter.register(HOME) { HomePresenter(registry) }
        Presenter.register(QUERY) { QueryPresenter(dispatcher, registry) }
        Presenter.register(DEPRECATION) { DeprecationPresenter(dispatcher, registry) }
        Presenter.register(DIFF) { DiffPresenter(dispatcher, registry) }
        Presenter.register(NEO4J) { Neo4jPresenter(registry) }
    }

    override val registry: Registry = ItemsStore { it.identifier }

    override val dispatcher: Dispatcher = Dispatcher(registry)

    override val bootstrapTasks = listOf { ReadRegistry(dispatcher, registry) }

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
}
