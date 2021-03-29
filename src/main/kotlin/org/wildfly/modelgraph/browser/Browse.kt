package org.wildfly.modelgraph.browser

import dev.fritz2.binding.RootStore
import dev.fritz2.binding.storeOf
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
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import org.patternfly.BreadcrumbStore
import org.patternfly.DoubleIcon
import org.patternfly.ItemsStore
import org.patternfly.SingleIcon
import org.patternfly.Size
import org.patternfly.TabStore
import org.patternfly.TreeBuilder
import org.patternfly.TreeItem
import org.patternfly.TreeStore
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
import org.patternfly.modifier
import org.patternfly.pageBreadcrumb
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
const val UI_TIMEOUT = 333L

enum class ResourceProperty(val key: String, val text: String) {
    ATTRIBUTES("attribute", "Attributes"),
    OPERATIONS("operation", "Operations"),
    CAPABILITIES("capability", "Capabilities")
}

sealed class ResourceState
object NoResourceDetails : ResourceState()
data class ResourceDetails(val resource: Resource) : ResourceState()

class BrowsePresenter : Presenter<BrowseView> {

    private val idProvider: IdProvider<Resource, String> = { it.id }
    val breadcrumbStore: BreadcrumbStore<Resource> = BreadcrumbStore(idProvider)
    val treeStore: TreeStore<Resource> = TreeStore(idProvider)
    val tabStore: TabStore<ResourceProperty> = TabStore { it.key }
    val resourceState: RootStore<ResourceState> = storeOf(NoResourceDetails)
    val attributesStore: ItemsStore<Attribute> = ItemsStore { it.id }
    val operationsStore: ItemsStore<Operation> = ItemsStore { it.id }
    val capabilitiesStore: ItemsStore<Capability> = ItemsStore { it.id }
    override val view: BrowseView = BrowseView(this)

    override fun bind() {
        attributesStore.pageSize(Int.MAX_VALUE)
        operationsStore.pageSize(Int.MAX_VALUE)
        capabilitiesStore.pageSize(Int.MAX_VALUE)

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
                ResourceDetails(Endpoints.resource(treeItem.unwrap().address))
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

    override fun show() {
        js(
            """
            var height = document.getElementById("mgb-browse-top").offsetHeight;
            document.documentElement.style.setProperty("--mgb-browse-top--Height", height + "px");
            """
        )
    }

    override fun prepareFromRequest(place: PlaceRequest) {
        val address = place.params["address"] ?: "/"
        val attribute = place.params["attribute"]
        val operation = place.params["operation"]
        val capability = place.params["capability"]
        MainScope().launch {
            val subtree = Endpoints.subtree(address)
            val tree = tree<Resource> {
                initialSelection = { it.unwrap().address == address }
                resourceItem(subtree, address)
            }
            treeStore.update(tree)
            if (attribute != null || operation != null || capability != null) {
                val (property, value) = when {
                    attribute != null -> ATTRIBUTES to attribute
                    operation != null -> OPERATIONS to operation
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
        groupChildren(parent, Endpoints.children(parent.address)).map { resource ->
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

class BrowseView(override val presenter: BrowsePresenter) : View, WithPresenter<BrowsePresenter> {

    override val content: ViewContent = {
        pageBreadcrumb(id = "mgb-browse-top", limitWidth = true) {
            breadcrumb(presenter.breadcrumbStore, noHomeLink = true) {
                display { +it.name }
            }
        }
        pageSection(
            limitWidth = true,
            baseClass = classes("light".modifier(), "overflow-scroll".modifier())
        ) {
            div(baseClass = "mgb-browse") {
                div(baseClass = classes("mgb-browse-tree", "mgb-browse-scroll", "mr-md".util())) {
                    treeView(presenter.treeStore, baseClass = "pt-0".util()) {
                        display { resource ->
                            span {
                                if (resource.deprecated) {
                                    // TODO Use custom CSS class
                                    domNode.style.textDecoration = "line-through"
                                }
                                if (resource.address == "/") +"Management Model" else +resource.name
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
                div(baseClass = classes("mgb-browse-resources", "mgb-browse-scroll")) {
                    textContent(baseClass = "mb-md".util()) {
                        p { presenter.treeStore.selected.map { it.unwrap().description }.asText() }
                    }
                    tabs(presenter.tabStore) {
                        hideIf(presenter.treeStore.selected.unwrap()) { it.singletonParent }
                        tabDisplay { +it.text }
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
                                    dataTable(
                                        presenter.attributesStore,
                                        baseClass = "compact".modifier()
                                    ) {
                                        dataTableColumns {
                                            dataTableColumn("Name") {
                                                headerClass("width-55".modifier())
                                                cellDisplay { attribute ->
                                                    div(
                                                        id = itemId(attribute),
                                                        baseClass = "mgb-highlightable"
                                                    ) {
                                                        domNode.dataset["attribute"] = attribute.name
                                                        strong { +attribute.name }
                                                    }
                                                    attribute.description?.let {
                                                        div { +it }
                                                    }
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
                                    dataTable(
                                        presenter.operationsStore,
                                        baseClass = "compact".modifier()
                                    ) {
                                        dataTableColumns {
                                            dataTableColumn("Name") {
                                                cellDisplay { operation ->
                                                    div(
                                                        id = itemId(operation),
                                                        baseClass = "mgb-highlightable"
                                                    ) {
                                                        domNode.dataset["operation"] = operation.name
                                                        strong { +operation.name }
                                                    }
                                                    operation.description?.let {
                                                        div { +it }
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
                                                cellDisplay { capability ->
                                                    a(
                                                        id = itemId(capability),
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
