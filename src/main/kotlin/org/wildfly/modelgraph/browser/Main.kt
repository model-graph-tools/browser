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
        skeleton()
    }
    AlertGroup.addToastAlertGroup()

    MainScope().launch {
        val registrations = cdi().dispatcher.registry()
        cdi().registry.update(registrations)
        if (registrations.isNotEmpty()) {
            cdi().activeRegistration.update(registrations[0])
        }
    }
}
