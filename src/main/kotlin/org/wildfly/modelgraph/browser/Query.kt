package org.wildfly.modelgraph.browser

import dev.fritz2.binding.EmittingHandler
import dev.fritz2.binding.Handler
import dev.fritz2.binding.Store
import dev.fritz2.binding.storeOf
import dev.fritz2.dom.html.Div
import dev.fritz2.dom.values
import dev.fritz2.mvp.Presenter
import dev.fritz2.mvp.View
import dev.fritz2.mvp.ViewContent
import dev.fritz2.mvp.WithPresenter
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.patternfly.ButtonVariation.control
import org.patternfly.ItemsStore
import org.patternfly.Sticky.TOP
import org.patternfly.aria
import org.patternfly.classes
import org.patternfly.dataList
import org.patternfly.dataListContent
import org.patternfly.dataListControl
import org.patternfly.dataListExpandableContent
import org.patternfly.dataListItem
import org.patternfly.dataListRow
import org.patternfly.dataListToggle
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
import org.wildfly.modelgraph.browser.QueryView.State.*

class QueryPresenter(
    private val dispatcher: Dispatcher,
    private val registry: Registry
) : Presenter<QueryView> {

    override val view: QueryView = QueryView(this, registry)

    val store: ItemsStore<Model> = ItemsStore { it.id }
    val query: Store<String> = storeOf("")

    val updateAndEmitQuery: EmittingHandler<String, String> = with(query) {
        handleAndEmit { _, value ->
            emit(value)
            value
        }
    }

    private val executeQuery: Handler<String> = with(store) {
        handle { items, value ->
            console.log("Query using '$value'")
            val models = dispatcher.query(value)
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

        with(query) {
            // connect the two handlers
            updateAndEmitQuery.filter { it.isNotEmpty() } handledBy executeQuery
        }

        with(registry) {
            // execute query if a new WildFly version has been selected
            selection.filter { it.isNotEmpty() }.map { query.current }.filter { it.isNotEmpty() } handledBy executeQuery
        }
    }
}

class QueryView(
    override val presenter: QueryPresenter,
    private val registry: Registry
) : View, WithPresenter<QueryPresenter> {

    enum class State { INITIAL, NO_RESULTS, DATA_LIST }

    val state: Store<State> = storeOf(INITIAL)

    override val content: ViewContent = {
        noWildFly(registry)
        pageSection(baseClass = classes("sticky-top".modifier(), "light".modifier())) {
            hideIf(registry.isEmpty())
            inputGroup {
                inputFormControl(baseClass = "mgb-xl-input") {
                    placeholder("Search for resources, attributes, operations and capabilities.")
                    type("search")
                    value(presenter.query.data)
                    aria["invalid"] = false
                    changes.values() handledBy presenter.updateAndEmitQuery
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
                    p { +"Search for resources, attributes, operations and capabilities." }
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
                presenter.query.data.asText()
                +"'. Please try another query."
            })
        }
        pageSection {
            hideIf(registry.isEmpty()
                .combine(state.data.map { it != DATA_LIST }) { noRegistry, noDataList ->
                    noRegistry || noDataList
                })
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
