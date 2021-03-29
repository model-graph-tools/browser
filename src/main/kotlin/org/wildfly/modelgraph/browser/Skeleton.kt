package org.wildfly.modelgraph.browser

import dev.fritz2.dom.html.RenderContext
import dev.fritz2.mvp.managedBy
import dev.fritz2.mvp.placeRequest
import org.patternfly.brand
import org.patternfly.horizontalNavigation
import org.patternfly.item
import org.patternfly.items
import org.patternfly.notificationBadge
import org.patternfly.page
import org.patternfly.pageHeader
import org.patternfly.pageHeaderTools
import org.patternfly.pageHeaderToolsItem
import org.patternfly.pageMain

fun RenderContext.skeleton() {
    page {
        pageHeader(id = "mgb-masthead") {
            brand {
                link {
                    href("#home")
                }
                img {
                    src("./model-graph-browser.svg")
                }
            }
            horizontalNavigation(cdi().placeManager.router) {
                items {
                    item(placeRequest(BROWSE), "Browse")
                    item(placeRequest(QUERY), "Query")

                }
            }
            pageHeaderTools {
                pageHeaderToolsItem {
                    notificationBadge()
                }
            }
        }
        pageMain(id = "main") {
            managedBy(cdi().placeManager)
        }
    }
}
