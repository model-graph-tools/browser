package org.wildfly.modelgraph.browser

import dev.fritz2.binding.Handler
import dev.fritz2.binding.RootStore
import dev.fritz2.binding.storeOf
import dev.fritz2.dom.html.RenderContext
import dev.fritz2.lenses.IdProvider
import dev.fritz2.mvp.PlaceRequest
import dev.fritz2.mvp.Presenter
import dev.fritz2.mvp.View
import dev.fritz2.mvp.ViewContent
import dev.fritz2.mvp.WithPresenter
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import org.patternfly.BreadcrumbStore
import org.patternfly.DoubleIcon
import org.patternfly.ItemsStore
import org.patternfly.Severity
import org.patternfly.SingleIcon
import org.patternfly.Size
import org.patternfly.TabStore
import org.patternfly.TreeBuilder
import org.patternfly.TreeItem
import org.patternfly.TreeStore
import org.patternfly.WithIdProvider
import org.patternfly.alert
import org.patternfly.breadcrumb
import org.patternfly.children
import org.patternfly.classes
import org.patternfly.dataTable
import org.patternfly.dataTableColumn
import org.patternfly.dataTableColumns
import org.patternfly.dom.By
import org.patternfly.dom.Id
import org.patternfly.dom.hideIf
import org.patternfly.dom.minusAssign
import org.patternfly.dom.plusAssign
import org.patternfly.dom.querySelector
import org.patternfly.dom.showIf
import org.patternfly.emptyState
import org.patternfly.emptyStateBody
import org.patternfly.fas
import org.patternfly.icon
import org.patternfly.item
import org.patternfly.items
import org.patternfly.layout
import org.patternfly.modifier
import org.patternfly.pageSection
import org.patternfly.tabs
import org.patternfly.textContent
import org.patternfly.tree
import org.patternfly.treeItem
import org.patternfly.treeView
import org.patternfly.unwrap
import org.patternfly.util
import org.w3c.dom.set
import org.wildfly.modelgraph.browser.ResourceProperty.ATTRIBUTES
import org.wildfly.modelgraph.browser.ResourceProperty.CAPABILITIES
import org.wildfly.modelgraph.browser.ResourceProperty.OPERATIONS

const val HIGHLIGHT_TIMEOUT = 2000L
const val UI_TIMEOUT = 666L

enum class ResourceProperty(val key: String, val text: String) {
    ATTRIBUTES("attribute", "Attributes"),
    OPERATIONS("operation", "Operations"),
    CAPABILITIES("capability", "Capabilities")
}

sealed class ResourceState
object NoResourceDetails : ResourceState()
data class ResourceDetails(val resource: Resource) : ResourceState()

class BrowsePresenter(
    private val dispatcher: Dispatcher,
    private val registry: Registry
) : Presenter<BrowseView> {

    private val idProvider: IdProvider<Resource, String> = { it.id }
    private var address: String = ""
    private var attribute: String? = null
    private var operation: String? = null
    private var capability: String? = null

    override val view: BrowseView = BrowseView(this, registry)

    val breadcrumbStore: BreadcrumbStore<Resource> = BreadcrumbStore(idProvider)
    val treeStore: TreeStore<Resource> = TreeStore(idProvider)
    val tabStore: TabStore<ResourceProperty> = TabStore { it.key }
    val resourceState: RootStore<ResourceState> = storeOf(NoResourceDetails)
    val attributesStore: ItemsStore<Attribute> = ItemsStore { it.id }
    val operationsStore: ItemsStore<Operation> = ItemsStore { it.id }
    val capabilitiesStore: ItemsStore<Capability> = ItemsStore { it.id }

    val updateTree: Handler<Unit> = with(treeStore) {
        handle {
            val subtree = dispatcher.subtree("/")
            tree {
                initialSelection = { true }
                resourceItem(subtree, "/")
            }
        }
    }

    override fun bind() {
        attributesStore.pageSize(Int.MAX_VALUE)
        operationsStore.pageSize(Int.MAX_VALUE)
        capabilitiesStore.pageSize(Int.MAX_VALUE)

        with(registry) {
            // update tree if a new WildFly version has been selected
            selection.filter { it.isNotEmpty() }.map { } handledBy updateTree

        }

        with(breadcrumbStore) {
            // breadcrumb:click -> treeItem:show
            clicked.unwrap() handledBy treeStore.showItem
        }

        with(treeStore) {
            // treeItem:select -> breadcrumb:update
            selected.map {
                items(idProvider, breadcrumbStore.itemSelection) {
                    val resources = it.path.unwrap()
                    resources.forEachIndexed { index, resource ->
                        item(resource) {
                            if (index == resources.size - 1) {
                                selected = true
                            }
                        }
                    }
                }
            } handledBy breadcrumbStore.update

            // treeItem:select -> resource:load
            selected.filterNot { treeItem ->
                treeItem.unwrap().singletonParent
            }.map { treeItem ->
                ResourceDetails(dispatcher.resource(treeItem.unwrap().address))
            } handledBy resourceState.update
        }

        with(resourceState) {
            // resource:loaded -> attributes:update
            data.filterIsInstance<ResourceDetails>().map { details ->
                details.resource.attributes.sortedBy { it.name }
            } handledBy attributesStore.addAll

            // resource:loaded -> operations:update
            data.filterIsInstance<ResourceDetails>().map { details ->
                details.resource.operations.sortedBy { it.name }
            } handledBy operationsStore.addAll

            // resource:loaded -> capabilities:update
            data.filterIsInstance<ResourceDetails>().map { details ->
                details.resource.capabilities.sortedBy { it.name }
            } handledBy capabilitiesStore.addAll
        }
    }

    override fun prepareFromRequest(place: PlaceRequest) {
        address = place.params["address"] ?: "/"
        attribute = place.params["attribute"]
        operation = place.params["operation"]
        capability = place.params["capability"]
    }

    override fun show() {
        if (document.getElementById("mgb-browse-top") != null) {
            js(
                """
            var height = document.getElementById("mgb-browse-top").offsetHeight;
            document.documentElement.style.setProperty("--mgb-browse-top--Height", height + "px");
            """
            )
        }
        MainScope().launch {
            val subtree = dispatcher.subtree(address)
            val tree = tree<Resource> {
                initialSelection = { it.unwrap().address == address }
                resourceItem(subtree, address)
            }
            treeStore.update(tree)
            if (attribute != null || operation != null || capability != null) {
                val (property, value) = when {
                    attribute != null -> ATTRIBUTES to attribute!!
                    operation != null -> OPERATIONS to operation!!
                    else -> CAPABILITIES to capability!!
                }
                resourceState.data.filterIsInstance<ResourceDetails>().take(1).collect {
                    tabStore.selectItem(property)
                    delay(UI_TIMEOUT)
                    highlight(property.key, value)
                }
            }
        }
    }

    private suspend fun highlight(attribute: String, value: String) {
        document.querySelector(By.data(attribute, value))?.let { cell ->
            cell.closest("tr")?.let { tr ->
                tr.scrollIntoView()
                tr.classList += "mgb-highlightable"
                tr.classList += "mgb-highlight"
                delay(HIGHLIGHT_TIMEOUT)
                tr.classList -= "mgb-highlight"
                delay(HIGHLIGHT_TIMEOUT)
                tr.classList -= "mgb-highlightable"
            }
        }
    }

    private fun TreeBuilder<Resource>.resourceItem(resource: Resource, address: String) {
        treeItem(resource) {
            val groupedResource = if (resource.singletonParent) {
                resource
            } else {
                resource.copy(children = groupChildren(resource, resource.children))
            }
            if (groupedResource.children.isNotEmpty()) {
                children {
                    groupedResource.children.forEach {
                        resourceItem(it, address)
                    }
                }
            }
        }
    }

    suspend fun readChildren(parent: Resource): List<TreeItem<Resource>> =
        groupChildren(parent, dispatcher.children(parent.address)).map { resource ->
            treeItem(resource) {
                if (resource.children.isNotEmpty()) {
                    children {
                        resource.children.forEach { child -> treeItem(child) }
                    }
                }
            }
        }

    private fun groupChildren(parent: Resource, children: List<Resource>): List<Resource> {
        val (singletons, resources) = children.partition { it.singleton }
        val singletonsWithChildren = singletons
            .groupBy { it.singletonParentName }
            .map { (singletonName, singletonChildren) ->
                Resource(
                    id = Id.unique(singletonName),
                    name = singletonName,
                    modelType = SINGLETON_PARENT_TYPE,
                    address = "${parent.address}/$singletonName",
                    singleton = true,
                    description = parent.childDescriptions[singletonName],
                    children = singletonChildren.map { it.copy(name = it.singletonChildName) }
                )
            }
        return (resources + singletonsWithChildren).sortedBy { it.name }
    }
}

class BrowseView(
    override val presenter: BrowsePresenter,
    private val registry: Registry
) : View, WithPresenter<BrowsePresenter> {

    override val content: ViewContent = {
        noWildFly(registry)
        pageSection(baseClass = classes("light".modifier(), "grid".layout(), "gutter".modifier())) {
            hideIf(registry.isEmpty())
            div(id = "mgb-browse-top", baseClass = classes("grid".layout("item"), "12-col".modifier())) {
                breadcrumb(presenter.breadcrumbStore, noHomeLink = true) {
                    display { +it.name }
                }
            }
            div(baseClass = classes("grid".layout("item"), "4-col".modifier(), "mgb-browse-scroll")) {
                treeView(presenter.treeStore, baseClass = "pt-0".util()) {
                    display { resource ->
                        span(baseClass = classes {
                            +("mgb-deprecated" `when` resource.deprecated)
                        }) {
                            if (resource.address == "/") +"Management Model" else +resource.name
                            resource.deprecation?.let { deprecation ->
                                // TODO turn this into a tooltip
                                attr(
                                    "title",
                                    "Deprecated since ${deprecation.since}. Reason: ${deprecation.reason}"
                                )
                            }
                        }
                    }
                    fetchItems { treeItem ->
                        presenter.readChildren(treeItem.unwrap())
                    }
                    iconProvider { resource ->
                        when {
                            resource.address == "/" -> {
                                SingleIcon { icon("sitemap".fas()) }
                            }
                            resource.singletonParent -> {
                                SingleIcon { icon("list".fas()) }
                            }
                            resource.singleton -> {
                                SingleIcon { icon("file-alt".fas()) }
                            }
                            else -> {
                                DoubleIcon(
                                    { icon("folder".fas()) },
                                    { icon("folder-open".fas()) }
                                )
                            }
                        }
                    }
                }
            }
            div(baseClass = classes("grid".layout("item"), "8-col".modifier(), "mgb-browse-scroll")) {
                textContent(baseClass = "mb-md".util()) {
                    p { presenter.treeStore.selected.map { it.unwrap().description }.asText() }
                }
                alert {
                    showIf(presenter.treeStore.selected.map { it.unwrap().deprecated })
                    inline(true)
                    severity(Severity.WARNING)
                    title("Deprecated")
                    content {
                        p {
                            presenter.treeStore.selected.map { it.unwrap().deprecation }.filterNotNull()
                                .map { it.reason }
                                .asText()
                        }
                        p {
                            +"Since "
                            presenter.treeStore.selected.map { it.unwrap().deprecation }.filterNotNull()
                                .map { it.since }
                                .asText()
                        }
                    }
                }
                tabs(presenter.tabStore) {
                    tabDisplay { +it.text }
                    hideIf(presenter.treeStore.selected.unwrap()) { it.singletonParent }
                    items {
                        item(ATTRIBUTES) {
                            div(baseClass = "mt-lg".util()) {
                                showIf(presenter.resourceState.data) {
                                    it is ResourceDetails && it.resource.attributes.isEmpty()
                                }
                                emptyState(
                                    size = Size.XS,
                                    iconClass = "ban".fas(),
                                    title = "No attributes"
                                ) {
                                    emptyStateBody {
                                        +"This resource does not contain any attributes."
                                    }
                                }
                            }
                            div {
                                showIf(presenter.resourceState.data) {
                                    it is ResourceDetails && it.resource.attributes.isNotEmpty()
                                }
                                div {
                                    // TODO Toolbar to filter attributes by
                                    //  - name
                                    //  - storage
                                    //  - access type
                                    dataTable(
                                        presenter.attributesStore,
                                        baseClass = "compact".modifier()
                                    ) {
                                        dataTableColumns {
                                            dataTableColumn("Name") {
                                                headerClass("width-55".modifier())
                                                cellDisplay { attribute ->
                                                    attribute(this@dataTableColumn, attribute)
                                                }
                                            }
                                            dataTableColumn("Type") {
                                                headerClass("width-15".modifier())
                                                cellDisplay { attribute -> +attribute.type }
                                            }
                                            dataTableColumn("Storage") {
                                                headerClass("width-15".modifier())
                                                cellDisplay { attribute -> +(attribute.storage ?: "n/a") }
                                            }
                                            dataTableColumn("Access Type") {
                                                headerClass("width-15".modifier())
                                                cellDisplay { attribute -> +(attribute.accessType ?: "n/a") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item(OPERATIONS) {
                            div(baseClass = "mt-lg".util()) {
                                showIf(presenter.resourceState.data) {
                                    it is ResourceDetails && it.resource.operations.isEmpty()
                                }
                                emptyState(
                                    size = Size.XS,
                                    iconClass = "ban".fas(),
                                    title = "No operations"
                                ) {
                                    emptyStateBody {
                                        +"This resource does not provide any operations."
                                    }
                                }
                            }
                            div {
                                showIf(presenter.resourceState.data) {
                                    it is ResourceDetails && it.resource.operations.isNotEmpty()
                                }
                                div {
                                    // TODO Toolbar to filter operations by
                                    //  - name
                                    //  - global / non-global
                                    //  - configuration / runtime
                                    dataTable(
                                        presenter.operationsStore,
                                        baseClass = "compact".modifier()
                                    ) {
                                        dataTableColumns {
                                            dataTableColumn("Name") {
                                                headerClass("width-35".modifier())
                                                cellDisplay { operation ->
                                                    operation(this@dataTableColumn, operation)
                                                }
                                            }
                                            dataTableColumn("Parameter") {
                                                headerClass("width-45".modifier())
                                                cellDisplay { operation ->
                                                    parameter(operation)
                                                }
                                            }
                                            dataTableColumn("Result") {
                                                headerClass("width-20".modifier())
                                                cellDisplay { operation ->
                                                    result(operation)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item(CAPABILITIES) {
                            div(baseClass = "mt-lg".util()) {
                                showIf(presenter.resourceState.data) {
                                    it is ResourceDetails && it.resource.capabilities.isEmpty()
                                }
                                emptyState(
                                    size = Size.XS,
                                    iconClass = "ban".fas(),
                                    title = "No capabilities"
                                ) {
                                    emptyStateBody {
                                        +"This resource does not provide any capabilities."
                                    }
                                }
                            }
                            div {
                                showIf(presenter.resourceState.data) {
                                    it is ResourceDetails && it.resource.capabilities.isNotEmpty()
                                }
                                dataTable(
                                    presenter.capabilitiesStore,
                                    baseClass = "compact".modifier()
                                ) {
                                    dataTableColumns {
                                        dataTableColumn("Name") {
                                            cellDisplay {
                                                capability(this@dataTableColumn, it)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun RenderContext.attribute(idProvider: WithIdProvider<Attribute>, attribute: Attribute) {
        div(
            id = idProvider.itemId(attribute),
            baseClass = classes {
                +"mgb-highlightable"
                +("mgb-deprecated" `when` attribute.deprecated)
            }
        ) {
            domNode.dataset["attribute"] = attribute.name
            strong {
                +attribute.name
                attribute.deprecation?.let { deprecation ->
                    // TODO turn this into a tooltip
                    attr(
                        "title",
                        "Deprecated since ${deprecation.since}. Reason: ${deprecation.reason}"
                    )
                }
            }
        }
        attribute.description?.let {
            div { +it }
        }
    }

    private fun RenderContext.operation(idProvider: WithIdProvider<Operation>, operation: Operation) {
        div(
            id = idProvider.itemId(operation),
            baseClass = classes {
                +"mgb-highlightable"
                +("mgb-deprecated" `when` operation.deprecated)
            }) {
            domNode.dataset["operation"] = operation.name
            strong {
                +operation.name
                operation.deprecation?.let { deprecation ->
                    // TODO turn this into a tooltip
                    attr(
                        "title",
                        "Deprecated since ${deprecation.since}. Reason: ${deprecation.reason}"
                    )
                }
            }
        }
        operation.description?.let {
            div { +it }
        }
    }

    private fun RenderContext.parameter(operation: Operation) {
        if (operation.parameters.isNotEmpty()) {
            ul {
                for (parameter in operation.parameters) {
                    li {
                        span(baseClass = classes {
                            +("mgb-deprecated" `when` parameter.deprecated)
                        }) {
                            +parameter.name
                            if (parameter.deprecated) {
                                parameter.deprecation?.let { deprecation ->
                                    // TODO turn this into a tooltip
                                    attr(
                                        "title",
                                        "Deprecated since ${deprecation.since}. Reason: ${deprecation.reason}"
                                    )
                                }
                            } else if (parameter.unit != null) {
                                attr("title", parameter.unit)
                            }
                        }
                        +": "
                        // TODO Nested parameters
                        +parameter.type
                        if (parameter.valueType != null) {
                            +"<${parameter.valueType}>"
                        }
                        br {}
                        parameter.description?.let { +it }
                    }
                }
            }
        }
    }

    private fun RenderContext.result(operation: Operation) {
        operation.returnValue?.let {
            div {
                +it
                if (operation.valueType != null) {
                    +"<${operation.valueType}>"
                }
            }
        }
    }

    private fun RenderContext.capability(idProvider: WithIdProvider<Capability>, capability: Capability) {
        a(
            id = idProvider.itemId(capability),
            baseClass = "mgb-highlightable"
        ) {
            domNode.dataset["capability"] = capability.name
            href(
                "$CAPABILITY_BASE/${
                    capability.name.replace(
                        '.',
                        '/'
                    )
                }/capability.adoc"
            )
            target("capability")
            +capability.name
        }
    }
}
