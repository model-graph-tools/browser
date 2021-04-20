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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.patternfly.ButtonVariation.control
import org.patternfly.DataListCell
import org.patternfly.ItemsStore
import org.patternfly.Sticky.TOP
import org.patternfly.badge
import org.patternfly.classes
import org.patternfly.dataList
import org.patternfly.dataListCell
import org.patternfly.dataListContent
import org.patternfly.dataListControl
import org.patternfly.dataListExpandableContent
import org.patternfly.dataListItem
import org.patternfly.dataListRow
import org.patternfly.dataListToggle
import org.patternfly.dom.aria
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

class QueryPresenter(private val dispatcher: Dispatcher) : Presenter<QueryView> {

    override val view: QueryView = QueryView(this)
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
}

class QueryView(override val presenter: QueryPresenter) : View, WithPresenter<QueryPresenter> {

    enum class State { INITIAL, NO_RESULTS, DATA_LIST }

    val state: RootStore<State> = storeOf(INITIAL)

    override val content: ViewContent = {
        pageSection(sticky = TOP, baseClass = "light".modifier()) {
            inputGroup {
                inputFormControl(baseClass = "mgb-query-input") {
                    placeholder("Search for resources, attributes, operations and capabilities")
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
            classMap(state.data.map { mapOf("display-none".util() to (it != INITIAL)) })
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
            classMap(state.data.map { mapOf("display-none".util() to (it != NO_RESULTS)) })
            emptyStateNoResults(body = {
                +"No results found for '"
                presenter.currentQuery.asText()
                +"'. Please try another query."
            })
        }
        pageSection {
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
                                dataListCell(baseClass = "no-fill".modifier()) {
                                    badge(baseClass = "mgb-${model.modelType.toLowerCase()}") {
                                        read(false)
                                        domNode.title = model.modelType
                                    }
                                }
                                dataListCell(id = itemId(model), baseClass = "align-left".modifier()) {
                                    when (model) {
                                        is Attribute -> attributeCell(model)
                                        is Capability -> capabilityCell(model)
                                        is Operation -> operationCell(model)
                                        is Resource -> resourceCell(model)
                                        else -> !"Unsupported model type ${model.modelType}"
                                    }
                                }
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

    private fun DataListCell<Model>.attributeCell(attribute: Attribute) {
        span(baseClass = classes("mgb-secondary", "mr-sm".util())) {
            +type(attribute)
        }
        span(baseClass = classes("mgb-primary", "mr-sm".util())) {
            if (attribute.definedIn != null) {
                a {
                    +attribute.name
                    href(browseAttribute(attribute.definedIn, attribute.name).hash)
                }
            } else {
                +attribute.name
            }
        }
        if (attribute.definedIn != null) {
            span(baseClass = classes("mgb-comment", "mr-sm".util())) {
                +" defined in "
            }
            span(baseClass = "mgb-secondary") {
                a {
                    +attribute.definedIn
                    href(browseResource(attribute.definedIn).hash)
                }
            }
        }
    }

    private fun DataListCell<Model>.capabilityCell(capability: Capability) {
        a {
            +capability.name
            href(browseCapability(capability.name).hash)
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

    private fun DataListCell<Model>.operationCell(operation: Operation) {
        if (operation.providedBy != null) {
            span(baseClass = "mgb-secondary") {
                a {
                    +operation.providedBy
                    href(browseResource(operation.providedBy).hash)
                }
            }
        }
        span(baseClass = "mgb-secondary") { +":" }
        span(baseClass = "mgb-primary") {
            if (operation.providedBy != null) {
                a {
                    +operation.name
                    href(browseOperation(operation.providedBy, operation.name).hash)
                }
            } else {
                +operation.name
            }
        }
        span(baseClass = "mgb-secondary") {
            +operation.parameters.joinToString(", ", "(", ")") {
                "${type(it)} ${it.name}"
            }
        }
    }

    private fun DataListCell<Model>.resourceCell(resource: Resource) {
        a {
            +resource.address
            href(browseResource(resource.address).hash)
        }
    }

    fun type(typed: Typed): String = buildString {
        append(typed.type)
        if (typed.valueType != null) {
            append("<").append(typed.valueType).append(">")
        }
    }
}
