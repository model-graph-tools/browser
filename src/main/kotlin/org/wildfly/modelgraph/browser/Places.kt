package org.wildfly.modelgraph.browser

import dev.fritz2.mvp.PlaceRequest
import dev.fritz2.mvp.Presenter
import dev.fritz2.mvp.placeRequest

const val BROWSE = "browse"
const val HOME = "home"
const val QUERY = "query"

fun registerPresenters() {
    Presenter.register(BROWSE) { BrowsePresenter(cdi().dispatcher) }
    Presenter.register(HOME, ::HomePresenter)
    Presenter.register(QUERY) { QueryPresenter(cdi().dispatcher) }
}

fun browseAttribute(address: String, attribute: String): PlaceRequest =
    placeRequest(BROWSE, Pair("address", address), Pair("attribute", attribute))

fun browseCapability(capability: String): PlaceRequest =
    placeRequest(BROWSE, Pair("capability", capability))

fun browseOperation(address: String, operation: String): PlaceRequest =
    placeRequest(BROWSE, Pair("address", address), Pair("operation", operation))

fun browseResource(address: String): PlaceRequest =
    placeRequest(BROWSE, Pair("address", address))
