package org.wildfly.modelgraph.browser

import dev.fritz2.mvp.Presenter
import dev.fritz2.mvp.View
import dev.fritz2.mvp.ViewContent
import kotlinx.coroutines.flow.map
import org.patternfly.dom.hideIf
import org.patternfly.modifier
import org.patternfly.pageSection
import org.patternfly.textContent
import org.patternfly.title

class Neo4jPresenter(registry: Registry) : Presenter<Neo4jView> {
    override val view: Neo4jView = Neo4jView(registry)
}

class Neo4jView(registry: Registry) : View {
    override val content: ViewContent = {
        noWildFly(registry)
        pageSection(baseClass = "light".modifier()) {
            hideIf(registry.isEmpty())
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
                    +"Click on the screenshot below to open the Neo4j browser for "
                    strong {
                        registry.failSafeSelection().map { "${it.productName} ${it.productVersion}" }.asText()
                    }
                    +" in a new browser tab. Please choose "
                }
                ul {
                    li {
                        em {
                            registry.failSafeSelection().map { it.neo4jBoltUri }.asText()
                        }
                        +" as connect URL (should be pre-selected) and"
                    }
                    li {
                        em { +"No authentication" }
                        +" as authentication type"
                    }
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
                        target(registry.failSafeSelection().map {
                            "neo4j-browser-${it.identifier}"
                        })
                        href(registry.failSafeSelection().map {
                            "${it.neo4jBrowserUri}?connectURL=${it.neo4jBoltUri}&cmd=connect"
                        })
                    }
                }
            }
        }
    }
}
