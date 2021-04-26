package org.wildfly.modelgraph.browser

import dev.fritz2.dom.html.render
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.patternfly.AlertGroup

external fun require(name: String): dynamic

fun main() {
    require("@patternfly/patternfly/patternfly.css")
    require("@patternfly/patternfly/patternfly-addons.css")

    registerPresenters()
    render {
        skeleton(cdi().registry)
    }
    AlertGroup.addToastAlertGroup()

    MainScope().launch {
        val registrations = cdi().dispatcher.registry()
            .distinctBy { it.identifier }
            .sortedBy { it.identifier }
            .reversed()
        cdi().registry.addAll(registrations)
        if (registrations.isNotEmpty()) {
            cdi().registry.selectOnly(registrations[0])
        }
    }
}
