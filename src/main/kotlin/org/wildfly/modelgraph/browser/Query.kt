package org.wildfly.modelgraph.browser

import dev.fritz2.binding.Handler
import dev.fritz2.binding.RootStore
import dev.fritz2.binding.storeOf
import dev.fritz2.dom.html.Div
import dev.fritz2.dom.values
import dev.fritz2.mvp.Presenter
import dev.fritz2.mvp.View
import dev.fritz2.mvp.ViewContent
import dev.fritz2.mvp.WithPresenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.patternfly.ButtonVariation.control
import org.patternfly.ItemsStore
import org.patternfly.Sticky.TOP
import org.patternfly.classes
import org.patternfly.dataList
import org.patternfly.dataListContent
import org.patternfly.dataListControl
import org.patternfly.dataListExpandableContent
import org.patternfly.dataListItem
import org.patternfly.dataListRow
import org.patternfly.dataListToggle
import org.patternfly.dom.aria
import org.patternfly.dom.hideIf
import org.patternfly.emptyState
import org.patternfly.emptyStateBody
import org.patternfly.emptyStateNoResults
import org.patternfly.fas
import org.patternfly.icon
import org.patternfly.inputFormControl
import org.patternfly.inputGroup
import org.patternfly.modifier
import org.patternfly.pageSection
import org.patternfly.pagination
import org.patternfly.pushButton
import org.patternfly.toolbar
import org.patternfly.toolbarContent
import org.patternfly.toolbarContentSection
import org.patternfly.toolbarItem
import org.patternfly.util
import org.wildfly.modelgraph.browser.QueryView.State.DATA_LIST
import org.wildfly.modelgraph.browser.QueryView.State.INITIAL
import org.wildfly.modelgraph.browser.QueryView.State.NO_RESULTS

class QueryPresenter(
    private val dispatcher: Dispatcher,
    private val registry: Registry
) : Presenter<QueryView> {

    override val view: QueryView = QueryView(this, registry)

    val store: ItemsStore<Model> = ItemsStore { it.id }
    val currentQuery: MutableStateFlow<String> = MutableStateFlow("")

    val query: Handler<String> = with(store) {
        handle { items, name ->
            currentQuery.value = name
            val models = dispatcher.query(name)
            if (models.size == 0) {
                view.state.update(NO_RESULTS)
            } else {
                view.state.update(DATA_LIST)
            }
            items.addAll(models.models)
        }
    }

    override fun bind() {
        store.pageSize(Int.MAX_VALUE)

        with(registry) {
            // update query if a new WildFly version has been selected
            selection.filter { it.isNotEmpty() }.map { currentQuery.value } handledBy query
        }
    }
}

class QueryView(
    override val presenter: QueryPresenter,
    private val registry: Registry
) : View, WithPresenter<QueryPresenter> {

    enum class State { INITIAL, NO_RESULTS, DATA_LIST }

    val state: RootStore<State> = storeOf(INITIAL)

    override val content: ViewContent = {
        noWildFly(registry)
        pageSection(sticky = TOP, baseClass = "light".modifier()) {
            hideIf(registry.isEmpty())
            inputGroup {
                inputFormControl(baseClass = "mgb-query-input") {
                    placeholder(
                        registry.failSafeSelection().map {
                            "Search for resources, attributes, operations and capabilities in ${it.productName} ${it.productVersion}"
                        }
                    )
                    type("search")
                    value(presenter.currentQuery)
                    aria["invalid"] = false
                    changes.values()
                        .filter { it.isNotEmpty() }
                        .handledBy(presenter.query)
                }
                pushButton(control) {
                    icon("search".fas())
                }
            }
        }
        pageSection(baseClass = classes("light".modifier(), "fill".modifier())) {
            hideIf(registry.isEmpty()
                .combine(state.data.map { it != INITIAL }) { noRegistry, notInitialState ->
                    noRegistry || notInitialState
                })
            emptyState(iconClass = "search".fas(), title = "Query") {
                emptyStateBody {
                    p {
                        +"Use this view to search for "
                        i { +"attributes" }
                        +", "
                        i { +"operations" }
                        +", "
                        i { +"resources" }
                        +" and "
                        i { +"capabilities" }
                        +"."
                    }
                }
            }
        }
        pageSection(baseClass = classes("light".modifier(), "fill".modifier())) {
            hideIf(registry.isEmpty()
                .combine(state.data.map { it != NO_RESULTS }) { noRegistry, resultsAvailable ->
                    noRegistry || resultsAvailable
                })
            emptyStateNoResults(body = {
                +"No results found for '"
                presenter.currentQuery.asText()
                +"'. Please try another query."
            })
        }
        pageSection {
            hideIf(registry.isEmpty()
                .combine(state.data.map { it != DATA_LIST }) { noRegistry, noDataList ->
                    noRegistry || noDataList
                })
            classMap(state.data.map { mapOf("display-none".util() to (it != DATA_LIST)) })
            toolbar {
                toolbarContent {
                    toolbarContentSection {
                        toolbarItem {
                            pagination(presenter.store)
                        }
                    }
                }
            }
            dataList(presenter.store) {
                display { model ->
                    dataListItem(model) {
                        dataListRow {
                            dataListControl {
                                dataListToggle()
                            }
                            dataListContent {
                                typeCell(model)
                                modelCell(model)
                            }
                        }
                        dataListExpandableContent {
                            when (model) {
                                is Attribute -> +(model.description ?: "No description available")
                                is Capability -> capabilityDescription(model)
                                is Operation -> +(model.description ?: "No description available")
                                is Resource -> +(model.description ?: "No description available")
                                else -> !"Unsupported model type ${model.modelType}"
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Div.capabilityDescription(capability: Capability) {
        +"Declared by "
        if (capability.declaredBy.size == 1) {
            a {
                +capability.declaredBy.first()
                href(browseResource(capability.declaredBy.first()).hash)
            }
        } else {
            ul {
                capability.declaredBy.forEach {
                    li {
                        a {
                            +it
                            href(browseResource(it).hash)
                        }
                    }
                }
            }
        }
    }
}
