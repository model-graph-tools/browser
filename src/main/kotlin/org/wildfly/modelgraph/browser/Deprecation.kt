package org.wildfly.modelgraph.browser

import dev.fritz2.binding.Handler
import dev.fritz2.dom.values
import dev.fritz2.mvp.Presenter
import dev.fritz2.mvp.View
import dev.fritz2.mvp.ViewContent
import dev.fritz2.mvp.WithPresenter
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.patternfly.ItemsStore
import org.patternfly.badge
import org.patternfly.dataList
import org.patternfly.dataListCell
import org.patternfly.dataListContent
import org.patternfly.dataListControl
import org.patternfly.dataListExpandableContent
import org.patternfly.dataListItem
import org.patternfly.dataListRow
import org.patternfly.dataListToggle
import org.patternfly.dom.hideIf
import org.patternfly.modifier
import org.patternfly.pageSection
import org.patternfly.pagination
import org.patternfly.selectFormControl
import org.patternfly.textContent
import org.patternfly.title
import org.patternfly.toolbar
import org.patternfly.toolbarContent
import org.patternfly.toolbarContentSection
import org.patternfly.toolbarItem

class DeprecationPresenter(
    private val dispatcher: Dispatcher,
    private val registry: Registry
) : Presenter<DeprecationView> {

    override val view: DeprecationView = DeprecationView(this, registry)

    var since: String? = null
    val modelStore: ItemsStore<Model> = ItemsStore { it.id }
    val versionStore: ItemsStore<Version> = ItemsStore { it.id }

    private val updateVersions: Handler<Unit> = with(versionStore) {
        handle { items ->
            items.addAll(dispatcher.versions())
        }
    }

    val updateDeprecations: Handler<String?> = with(modelStore) {
        handle { items, since ->
            this@DeprecationPresenter.since = since
            items.addAll(dispatcher.deprecated(since).models)
        }
    }

    override fun bind() {
        modelStore.pageSize(Int.MAX_VALUE)

        with(registry) {
            // update stores if a new WildFly version has been selected
            selection.filter { it.isNotEmpty() }.map { } handledBy updateVersions
            selection.filter { it.isNotEmpty() }.map { since } handledBy updateDeprecations
        }
    }

    override fun show() {
        updateVersions()
        updateDeprecations(since)
    }
}

class DeprecationView(
    override val presenter: DeprecationPresenter,
    private val registry: Registry
) : View, WithPresenter<DeprecationPresenter> {

    override val content: ViewContent = {
        noWildFly(registry)
        pageSection(baseClass = "light".modifier()) {
            hideIf(registry.isEmpty())
            textContent {
                title { +"Deprecated" }
                p {
                    +"List of deprecated attributes, operations and resources in "
                    strong {
                        registry.failSafeSelection().map { "${it.productName} ${it.productVersion}" }.asText()
                    }
                    +" which uses management model version "
                    strong {
                        registry.failSafeSelection().map { it.managementVersion }.asText()
                    }
                    +"."

                }
            }
        }
        pageSection {
            hideIf(registry.isEmpty())
            toolbar {
                toolbarContent {
                    toolbarContentSection {
                        toolbarItem {
                            selectFormControl {
                                inlineStyle("width: 10em")
                                changes.values() handledBy presenter.updateDeprecations
                                option {
                                    selected(presenter.since == null)
                                    value("")
                                    +"All deprecations"
                                }
                                presenter.versionStore.data.map { items ->
                                    items.all.sortedBy { it.ordinal() }
                                }.renderEach { version ->
                                    option {
                                        selected(presenter.since == version.toString())
                                        +version.toString()
                                    }
                                }
                            }
                        }
                        toolbarItem {
                            pagination(presenter.modelStore)
                        }
                    }
                }
            }
            dataList(presenter.modelStore) {
                display { model ->
                    dataListItem(model) {
                        dataListRow {
                            dataListControl {
                                dataListToggle()
                            }
                            dataListContent {
                                dataListCell(baseClass = "no-fill".modifier()) {
                                    badge {
                                        read(false)
                                        attr("title", "Deprecated since")
                                        value(
                                            when (model) {
                                                is Attribute -> model.deprecation?.since?.toString() ?: "n/a"
                                                is Operation -> model.deprecation?.since?.toString() ?: "n/a"
                                                is Resource -> model.deprecation?.since?.toString() ?: "n/a"
                                                else -> "n/a"
                                            }
                                        )
                                    }
                                }
                                typeCell(model)
                                modelCell(model)
                            }
                        }
                        dataListExpandableContent {
                            +(when (model) {
                                is Attribute -> model.deprecation?.reason ?: "No reason available"
                                is Operation -> model.deprecation?.reason ?: "No reason available"
                                is Resource -> model.deprecation?.reason ?: "No reason available"
                                else -> "No reason available"
                            })
                        }
                    }
                }
            }
        }
    }
}
