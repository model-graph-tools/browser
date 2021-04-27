package org.wildfly.modelgraph.browser

import dev.fritz2.dom.html.Div
import dev.fritz2.mvp.Presenter
import dev.fritz2.mvp.View
import dev.fritz2.mvp.ViewContent
import dev.fritz2.mvp.WithPresenter
import org.patternfly.DataListCell
import org.patternfly.ItemsStore
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
import org.patternfly.modifier
import org.patternfly.pageSection
import org.patternfly.pagination
import org.patternfly.toolbar
import org.patternfly.toolbarContent
import org.patternfly.toolbarContentSection
import org.patternfly.toolbarItem
import org.patternfly.util

class DeprecationPresenter(private val dispatcher: Dispatcher) : Presenter<DeprecationView> {

    override val view: DeprecationView = DeprecationView(this)
    val store: ItemsStore<Model> = ItemsStore { it.id }
}

class DeprecationView(override val presenter: DeprecationPresenter) : View, WithPresenter<DeprecationPresenter> {

    override val content: ViewContent = {
        pageSection {
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
