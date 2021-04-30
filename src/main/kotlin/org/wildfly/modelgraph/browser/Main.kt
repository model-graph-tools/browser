package org.wildfly.modelgraph.browser

import dev.fritz2.dom.html.render
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.patternfly.AlertGroup.Companion.addToastAlertGroup

external fun require(name: String): dynamic

fun main() {
    require("@patternfly/patternfly/patternfly.css")
    require("@patternfly/patternfly/patternfly-addons.css")

    render {
        skeleton(cdi().registry, cdi().placeManager)
    }
    addToastAlertGroup()

    MainScope().launch {
        cdi().bootstrapTasks.forEach {
            val bootstrapTask = it()
            console.log("Execute ${bootstrapTask.name}")
            bootstrapTask.execute()
        }
    }
}
