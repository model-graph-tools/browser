package org.wildfly.modelgraph.browser

import dev.fritz2.dom.html.render
import org.patternfly.AlertGroup.Companion.addToastAlertGroup

external fun require(name: String): dynamic

fun main() {
    require("@patternfly/patternfly/patternfly.css")
    require("@patternfly/patternfly/patternfly-addons.css")

    registerPresenters()
    render {
        skeleton(cdi().registry, cdi().placeManager)
    }
    addToastAlertGroup()
    pollRegistry(cdi().dispatcher, cdi().registry)
}
