package org.wildfly.modelgraph.browser

import dev.fritz2.dom.html.Div
import org.patternfly.DataListCell
import org.patternfly.DataListContent
import org.patternfly.badge
import org.patternfly.classes
import org.patternfly.dataListCell
import org.patternfly.modifier
import org.patternfly.util

fun DataListContent<Model>.typeCell(model: Model) {
    dataListCell(baseClass = "no-fill".modifier()) {
        badge(baseClass = "mgb-${model.modelType.toLowerCase()}") {
            read(false)
            attr("title", model.modelType)
        }
    }
}

fun DataListContent<Model>.modelCell(model: Model) {
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

private fun type(typed: Typed): String = buildString {
    append(typed.type)
    if (typed.valueType != null) {
        append("<").append(typed.valueType).append(">")
    }
}
