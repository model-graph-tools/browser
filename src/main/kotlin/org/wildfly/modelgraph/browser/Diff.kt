package org.wildfly.modelgraph.browser

import dev.fritz2.binding.EmittingHandler
import dev.fritz2.binding.Handler
import dev.fritz2.binding.Store
import dev.fritz2.binding.storeOf
import dev.fritz2.dom.html.RenderContext
import dev.fritz2.dom.html.TextElement
import dev.fritz2.dom.values
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import org.patternfly.ButtonVariant.plain
import org.patternfly.aria
import org.patternfly.classes
import org.patternfly.clickButton
import org.patternfly.component
import org.patternfly.dom.hideIf
import org.patternfly.emptyState
import org.patternfly.emptyStateBody
import org.patternfly.fas
import org.patternfly.inputFormControl
import org.patternfly.inputGroup
import org.patternfly.layout
import org.patternfly.modifier
import org.patternfly.mvp.Presenter
import org.patternfly.mvp.View
import org.patternfly.mvp.ViewContent
import org.patternfly.mvp.WithPresenter
import org.patternfly.pageSection
import org.patternfly.selectFormControl
import org.patternfly.util
import org.wildfly.modelgraph.browser.DiffView.State.DIFF
import org.wildfly.modelgraph.browser.DiffView.State.INITIAL
import org.wildfly.modelgraph.browser.DiffView.State.NO_DIFF

data class DiffRequest(val address: String, val from: String, val to: String) {
    fun isValid(): Boolean = (address.isNotEmpty() && from.isNotEmpty() && to.isNotEmpty()) && from != to
}

data class ResourceRequest(val address: String, val identifier: String) {
    fun isValid(): Boolean = address.isNotEmpty() && identifier.isNotEmpty()
}

class DiffPresenter(
    private val dispatcher: Dispatcher,
    private val registry: Registry
) : Presenter<DiffView> {

    override val view: DiffView = DiffView(this, registry)

    val address: Store<String> = storeOf("")
    val from: Store<String> = storeOf("")
    val to: Store<String> = storeOf("")
    val diff: Store<JsonArray> = storeOf(JsonArray(emptyList()))
    val fromResource: Store<JsonObject> = storeOf(JsonObject(emptyMap()))
    val toResource: Store<JsonObject> = storeOf(JsonObject(emptyMap()))

    val updateAddress: EmittingHandler<String, String> = with(address) {
        handleAndEmit { _, value ->
            emit(value)
            value
        }
    }

    val updateFrom: EmittingHandler<String, String> = with(from) {
        handleAndEmit { _, value ->
            emit(value)
            value
        }
    }

    val updateTo: EmittingHandler<String, String> = with(to) {
        handleAndEmit { _, value ->
            emit(value)
            value
        }
    }

    private val executeDiff: EmittingHandler<DiffRequest, Pair<ResourceRequest, ResourceRequest>> = with(diff) {
        handleAndEmit { diff, request ->
            if (request.isValid()) {
                emit(
                    ResourceRequest(request.address, request.from) to ResourceRequest(request.address, request.to)
                )
                dispatcher.diff(request.address, request.from, request.to).also { json ->
                    if (json.isEmpty()) {
                        view.state.update(NO_DIFF)
                    } else {
                        view.state.update(DIFF)
                    }
                }
            } else {
                diff
            }
        }
    }

    private val loadFrom: Handler<ResourceRequest> = with(fromResource) {
        handle { resource, request ->
            if (request.isValid()) {
                console.log("Load from $request")
                dispatcher.resourceForDiff(request.identifier, request.address)
            } else {
                resource
            }
        }
    }

    private val loadTo: Handler<ResourceRequest> = with(toResource) {
        handle { resource, request ->
            if (request.isValid()) {
                console.log("Load to $request")
                dispatcher.resourceForDiff(request.identifier, request.address)
            } else {
                resource
            }
        }
    }

    override fun bind() {
        // set current from and to values w/o executing diff
        with(from) {
            registry.failSafeSelection().map { it.identifier } handledBy update
        }
        with(to) {
            registry.failSafeSelection().map { it.identifier } handledBy update
        }

        // bind updates of address, from and to to execute diff handler
        with(address) {
            updateAddress.map { DiffRequest(it, from.current, to.current) } handledBy executeDiff
        }
        with(from) {
            updateFrom.map { DiffRequest(address.current, it, to.current) } handledBy executeDiff
        }
        with(to) {
            updateTo.map { DiffRequest(address.current, from.current, it) } handledBy executeDiff
        }

        // bind execute diff handler to load resources handlers
        with(diff) {
            executeDiff.map { it.first } handledBy loadFrom
            executeDiff.map { it.second } handledBy loadTo
        }
    }
}

@Suppress("DuplicatedCode")
class DiffView(
    override val presenter: DiffPresenter,
    private val registry: Registry
) : View, WithPresenter<DiffPresenter> {

    enum class State { INITIAL, NO_DIFF, DIFF }

    val state: Store<State> = storeOf(INITIAL)
    private val fromExpanded: Store<Boolean> = storeOf(true)
    private val resourceExpanded: Store<Boolean> = storeOf(true)
    private val toExpanded: Store<Boolean> = storeOf(true)

    override val content: ViewContent = {
        noWildFly(registry)
        pageSection(baseClass = classes("sticky-top".modifier(), "light".modifier())) {
            hideIf(registry.isEmpty())
            inputGroup {
                selectFormControl {
                    inlineStyle("width: 15em")
                    value(presenter.from.data)
                    changes.values() handledBy presenter.updateFrom
                    registry.data.map { it.items }.renderEach { registration ->
                        option {
                            value(registration.identifier)
                            +"${registration.productName} ${registration.productVersion}"
                        }
                    }
                }
                inputFormControl(baseClass = "mgb-xl-input") {
                    placeholder("address")
                    value(presenter.address.data)
                    aria["invalid"] = false
                    changes.values() handledBy presenter.updateAddress
                }
                selectFormControl {
                    inlineStyle("width: 15em")
                    value(presenter.to.data)
                    changes.values() handledBy presenter.updateTo
                    registry.data.map { it.items }.renderEach { registration ->
                        option {
                            value(registration.identifier)
                            +"${registration.productName} ${registration.productVersion}"
                        }
                    }
                }
            }
        }
        pageSection(baseClass = classes("light".modifier(), "fill".modifier())) {
            hideIf(registry.isEmpty()
                .combine(state.data.map { it != INITIAL }) { noRegistry, notInitialState ->
                    noRegistry || notInitialState
                })
            emptyState(iconClass = "not-equal".fas(), title = "Diff") {
                emptyStateBody {
                    div(baseClass = "bullseye".layout()) {
                        p(baseClass = classes("bullseye".layout("item"), "w-66".util())) {
                            +"Compare a resources between two different WildFly versions. The difference is shown as JSON patch document according to "
                            a {
                                +"RFC 6902"
                                target("rfc6902")
                                href("https://tools.ietf.org/html/rfc6902")
                            }
                            +"."
                            br {}
                            br {}
                            +"This format describes the operations necessary to transform the resource on the left into the resource on the right."
                        }
                    }
                }
            }
        }
        pageSection(baseClass = classes("light".modifier(), "fill".modifier())) {
            hideIf(registry.isEmpty()
                .combine(state.data.map { it != NO_DIFF }) { noRegistry, diff ->
                    noRegistry || diff
                })
            emptyState(iconClass = "equals".fas(), title = "Diff") {
                emptyStateBody {
                    p { +"No difference found." }
                }
            }
        }
        pageSection(baseClass = classes("light".modifier(), "fill".modifier())) {
            hideIf(registry.isEmpty()
                .combine(state.data.map { it != DIFF }) { noRegistry, noDiff ->
                    noRegistry || noDiff
                })
            div(
                baseClass = classes(
                    "flex".layout(),
                    "nowrap".modifier(),
                    "align-items-stretch".util()
                )
            ) {
                diffCode(fromExpanded) {
                    presenter.fromResource.data.map { json ->
                        jsonPrettyPrint.encodeToString(json)
                    }.asText()
                }
                diffCode(resourceExpanded) {
                    presenter.diff.data.map { json ->
                        jsonPrettyPrint.encodeToString(json)
                    }.asText()
                }
                diffCode(toExpanded) {
                    presenter.toResource.data.map { json ->
                        jsonPrettyPrint.encodeToString(json)
                    }.asText()
                }
            }
        }
    }

    private fun RenderContext.diffCode(expanded: Store<Boolean>, content: TextElement.() -> Unit) {
        div(
            baseClass = classes(
                "flex".layout("item"), "code-block".component(),
            )
        ) {
            classMap(expanded.data.map {
                mapOf(
                    classes("mgb-diff-code__expanded", "flex-1".modifier())!! to it,
                    classes("mgb-diff-code__collapsed", "flex-default".modifier())!! to !it
                )
            })
            div(baseClass = "code-block".component("header")) {
                div(baseClass = "code-block".component("actions")) {
                    div(baseClass = "code-block".component("actions", "item")) {
                        clickButton(plain) {
                            attr("title", expanded.data.map {
                                if (it) "collapse" else "expand"
                            })
                            icon("") {
                                iconClass(expanded.data.map {
                                    if (it) "compress".fas() else "expand".fas()
                                })
                            }
                        }.map { !expanded.current } handledBy expanded.update
                    }
                }
            }
            div(baseClass = "code-block".component("content")) {
                pre(baseClass = "code-block".component("pre")) {
                    code(baseClass = "code-block".component("code")) {
                        content(this)
                    }
                }
            }
        }
    }
}
