package org.wildfly.modelgraph.browser

import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.patternfly.ItemsStore

val WILDFLY_LINKS: Map<String, Links> = mapOf(
    "wildfly-full-23.0.0.Final-mgt-16.0.0" to Links(
        "https://www.wildfly.org/news/2021/03/11/WildFly23-Final-Released/",
        "https://docs.wildfly.org/23/"
    ),
    "wildfly-full-22.0.0.Final-mgt-15.0.0" to Links(
        "https://www.wildfly.org/news/2021/01/13/WildFly22-Final-Released/",
        "https://docs.wildfly.org/22/"
    ),
    "wildfly-full-21.0.0.Final-mgt-14.0.0" to Links(
        "https://www.wildfly.org/news/2020/10/13/WildFly21-Final-Released/",
        "https://docs.wildfly.org/21/"
    ),
    "wildfly-full-20.0.0.Final-mgt-13.0.0" to Links(
        "https://www.wildfly.org/news/2020/06/08/WildFly20-Final-Released/",
        "https://docs.wildfly.org/20/"
    ),
    "wildfly-full-19.0.0.Final-mgt-12.0.0" to Links(
        "https://www.wildfly.org/news/2020/03/18/WildFly19-Final-Released/",
        "https://docs.wildfly.org/19/"
    ),
    "wildfly-full-18.0.0.Final-mgt-11.0.0" to Links(
        "https://www.wildfly.org/news/2019/10/03/WildFly18-Final-Released/",
        "https://docs.wildfly.org/18/"
    ),
    "wildfly-full-17.0.0.Final-mgt-11.0.0" to Links(
        "https://www.wildfly.org/news/2019/06/10/WildFly17-Final-Released/",
        "https://docs.wildfly.org/17/"
    ),
    "wildfly-full-16.0.0.Final-mgt-11.0.0" to Links(
        "https://www.wildfly.org/news/2019/02/27/WildFly16-Final-Released/",
        "https://docs.wildfly.org/16/"
    ),
    "wildfly-full-15.0.0.Final-mgt-10.0.0" to Links(
        "https://www.wildfly.org/news/2018/12/13/WildFly15-Final-Released/",
        "https://docs.wildfly.org/15/"
    ),
    "wildfly-full-14.0.0.Final-mgt-9.0.0" to Links(
        "https://www.wildfly.org/news/2018/08/30/WildFly14-Final-Released/",
        "https://docs.wildfly.org/14/"
    ),
    "wildfly-full-13.0.0.Final-mgt-8.0.0" to Links(
        "https://www.wildfly.org/news/2018/05/31/WildFly13-Final-Released/",
        "https://docs.wildfly.org/13/"
    ),
    "wildfly-full-12.0.0.Final-mgt-7.0.0" to Links(
        "https://www.wildfly.org/news/2018/03/01/WildFly12-Final-Released/",
        "https://docs.wildfly.org/12/"
    ),
    "wildfly-full-11.0.0.Final-mgt-6.0.0" to Links(
        "https://www.wildfly.org/news/2017/10/24/WildFly11-Final-Released/",
        ""
    ),
    "wildfly-full-10.0.0.Final-mgt-5.0.0" to Links(
        "https://www.wildfly.org/news/2016/01/30/WildFly10-Released/",
        ""
    ),
)

private var pollingHandle: Int? = null
private const val POLLING_INTERVAL: Int = 3333

fun poll(dispatcher: Dispatcher, registry: ItemsStore<Registration>) {
    MainScope().launch {
        val registrations = dispatcher.registry()
            .distinctBy { it.identifier }
            .sortedBy { it.identifier }
            .reversed()
        val current = registry.current.selection.firstOrNull()
        registry.addAll(registrations)
        if (registrations.isNotEmpty()) {
            if (current != null && current in registrations) {
                registry.selectOnly(current)
            } else {
                registry.selectOnly(registrations[0])
            }
        }
    }
}

fun pollRegistry(dispatcher: Dispatcher, registry: ItemsStore<Registration>) {
    poll(dispatcher, registry)
    pollingHandle?.let { window.clearInterval(it) }
    pollingHandle = window.setInterval({ poll(dispatcher, registry) }, POLLING_INTERVAL)
}

@Serializable
data class Registration(
    val identifier: String,
    val productName: String,
    val productVersion: String,
    val managementVersion: String,
    val modelServiceUri: String,
    val neo4jBrowserUri: String,
    val neo4jBoltUri: String
) {
    override fun toString(): String = "$productName $productVersion"
}

data class Links(
    val releaseNotes: String,
    val documentation: String
)