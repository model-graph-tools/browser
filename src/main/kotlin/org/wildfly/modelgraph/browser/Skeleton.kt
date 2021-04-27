package org.wildfly.modelgraph.browser

import dev.fritz2.dom.html.RenderContext
import dev.fritz2.mvp.PlaceManager
import dev.fritz2.mvp.managedBy
import dev.fritz2.mvp.placeRequest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.patternfly.ContextSelectorStore
import org.patternfly.ItemsStore
import org.patternfly.brand
import org.patternfly.contextSelector
import org.patternfly.horizontalNavigation
import org.patternfly.item
import org.patternfly.items
import org.patternfly.notificationBadge
import org.patternfly.page
import org.patternfly.pageHeader
import org.patternfly.pageHeaderTools
import org.patternfly.pageHeaderToolsGroup
import org.patternfly.pageHeaderToolsItem
import org.patternfly.pageMain
import org.patternfly.unwrap

fun RenderContext.skeleton(registry: ItemsStore<Registration>, placeManager: PlaceManager) {
    val css = ContextSelectorStore<Registration>()

    // wire registry and context selector store
    with(registry) {
        // registry:update -> css:update
        data.map { items ->
            items(css.idProvider, css.itemSelection) {
                items.items.forEachIndexed { index, registration ->
                    item(registration) {
                        if (index == 0) {
                            selected = true
                        }
                    }
                }
            }
        } handledBy css.update

        // registry:select -> css:select
        selection.filter { it.isNotEmpty() }.map { it.first() } handledBy css.handleSelection
    }

    with(css) {
        // css:select -> registry:select
        singleSelection.unwrap() handledBy registry.selectOnly
    }

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
            horizontalNavigation(placeManager.router) {
                items {
                    item(placeRequest(BROWSE), "Browse")
                    item(placeRequest(QUERY), "Query")
                    item(placeRequest(DEPRECATION), "Deprecation")
                    item(placeRequest(NEO4J), "Neo4j")
                }
            }
            pageHeaderTools {
                pageHeaderToolsGroup {
                    pageHeaderToolsItem {
                        contextSelector(css)
                    }
                    pageHeaderToolsItem {
                        notificationBadge()
                    }
                }
            }
        }
        pageMain(id = "main") {
            managedBy(placeManager)
        }
    }
}
