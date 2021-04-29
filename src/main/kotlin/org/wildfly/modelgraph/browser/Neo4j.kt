package org.wildfly.modelgraph.browser

import dev.fritz2.mvp.Presenter
import dev.fritz2.mvp.View
import dev.fritz2.mvp.ViewContent
import kotlinx.coroutines.flow.map
import org.patternfly.ItemsStore
import org.patternfly.modifier
import org.patternfly.pageSection
import org.patternfly.textContent
import org.patternfly.title

class Neo4jPresenter(registry: ItemsStore<Registration>) : Presenter<Neo4jView> {
    override val view: Neo4jView = Neo4jView(registry)
}

class Neo4jView(registry: ItemsStore<Registration>) : View {
    override val content: ViewContent = {
        pageSection(baseClass = "light".modifier()) {
            textContent {
                title { +"Neo4j Browser" }
                p {
                    +"The Neo4j browser is a developer-focused tool to interact with a graph database. It allows developers to execute "
                    a {
                        +"Cypher"
                        target("neo4j")
                        href("https://neo4j.com/docs/cypher-refcard/current/")
                    }
                    +"  queries and visualize the results."
                }
                p {
                    +"Click on the screenshot below to open the Neo4j browser for the selected WildFly version in a new browser tab. Please choose "
                }
                ul {
                    li {
                        registry.data.map {
                            "${it.selection.first().neo4jBoltUri} as connect URL (should be preselected) and"
                        }.asText()
                    }
                    li { +"No authentication as authentication type" }
                }
                p {
                    +"The Neo4j browser will show a tutorial about the model graph database, once connected."
                }
                p {
                    a {
                        img {
                            src("/neo4j-browser.png")
                            inlineStyle("height: 300px")
                        }
                        target(registry.data.map { "neo4j-browser-${it.selection.first().identifier}" })
                        href(registry.data
                            .map { it.selection.first() }
                            .map { "${it.neo4jBrowserUri}?connectURL=${it.neo4jBoltUri}&cmd=connect" })
                    }
                }
            }
        }
    }
}
