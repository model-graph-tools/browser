package org.wildfly.modelgraph.browser

import dev.fritz2.mvp.Presenter
import dev.fritz2.mvp.View
import dev.fritz2.mvp.ViewContent
import org.patternfly.ButtonVariation
import org.patternfly.ItemsStore
import org.patternfly.linkButton
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
                val registration = registry.current.selection.first()
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
                    +"Use the link below to open the Neo4j browser for the current model graph database in a new browser tab. There's no user or password necessary - just hit connect. The Neo4j browser will show a tutorial about the model graph database, once connected."
                }
                p {
                    a {
                        img {
                            src("/neo4j-logo-color.svg")
                            inlineStyle("height: 32px")
                        }
                        target("neo4j-${registration.identifier}")
                        href("${registration.neo4jBrowserUri}?connectURL=${registration.neo4jBoltUri}")
                    }
                }
            }
        }
    }
}
