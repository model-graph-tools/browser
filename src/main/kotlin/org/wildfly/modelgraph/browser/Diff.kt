package org.wildfly.modelgraph.browser

import dev.fritz2.mvp.PlaceRequest
import dev.fritz2.mvp.Presenter
import dev.fritz2.mvp.View
import dev.fritz2.mvp.ViewContent
import dev.fritz2.mvp.WithPresenter
import org.patternfly.Sticky.TOP
import org.patternfly.dom.hideIf
import org.patternfly.modifier
import org.patternfly.pageSection
import org.patternfly.textContent
import org.patternfly.title

class DiffPresenter(
    private val dispatcher: Dispatcher,
    private val registry: Registry
) : Presenter<DiffView> {

    override val view: DiffView = DiffView(this, registry)
    var from: String? = null
    var address: String? = null
    var to: String? = null

    override fun prepareFromRequest(place: PlaceRequest) {
        from = place.params["from"]
        address = place.params["address"]
        to = place.params["to"]
    }
}

class DiffView(
    override val presenter: DiffPresenter,
    private val registry: Registry
) : View, WithPresenter<DiffPresenter> {

    override val content: ViewContent = {
        noWildFly(registry)
        pageSection(sticky = TOP, baseClass = "light".modifier()) {
            hideIf(registry.isEmpty())
            textContent {
                title { +"Diff" }
                p { +"Not yet implemented" }
            }
        }
    }
}
