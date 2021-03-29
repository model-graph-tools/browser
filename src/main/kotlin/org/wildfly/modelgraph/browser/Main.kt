package org.wildfly.modelgraph.browser

import dev.fritz2.dom.html.render
import org.patternfly.AlertGroup

external fun require(name: String): dynamic

fun main() {
    require("@patternfly/patternfly/patternfly.css")
    require("@patternfly/patternfly/patternfly-addons.css")

    registerPresenters()
    render {
        skeleton()
    }
    AlertGroup.addToastAlertGroup()
}
