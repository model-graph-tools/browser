package org.wildfly.modelgraph.browser

import org.patternfly.card
import org.patternfly.cardBody
import org.patternfly.cardFooter
import org.patternfly.cardTitle
import org.patternfly.cardView
import org.patternfly.classes
import org.patternfly.dom.hideIf
import org.patternfly.fas
import org.patternfly.icon
import org.patternfly.modifier
import org.patternfly.mvp.Presenter
import org.patternfly.mvp.View
import org.patternfly.mvp.ViewContent
import org.patternfly.pageSection
import org.patternfly.textContent
import org.patternfly.title
import org.patternfly.util

class HomePresenter(registry: Registry) : Presenter<HomeView> {
    override val view: HomeView = HomeView(registry)
}

class HomeView(registry: Registry) : View {
    override val content: ViewContent = {
        noWildFly(registry)
        pageSection(baseClass = "light".modifier()) {
            hideIf(registry.isEmpty())
            textContent {
                title { +"WildFly Model Graph Browser" }
                p { +"Query and browse the WildFly management model." }
                p { +"The cards below show the available WildFly versions. Please select a version you want to work with. You can switch the active version anytime using the context selector in the header." }
            }
        }
        pageSection(baseClass = classes("fill".modifier())) {
            hideIf(registry.isEmpty())
            cardView(cdi().registry, singleSelection = true) {
                display { registration ->
                    card(registration, selectable = true, baseClass = "hoverable".modifier()) {
                        cardTitle { +"${registration.productName} ${registration.productVersion}" }
                        cardBody { +"Management model ${registration.managementVersion}" }
                        WILDFLY_LINKS[registration.identifier]?.let { (releaseNotes, documentation) ->
                            if (releaseNotes.isNotEmpty() || documentation.isNotEmpty()) {
                                cardFooter {
                                    ul(baseClass = "font-size-sm".util()) {
                                        if (releaseNotes.isNotEmpty()) {
                                            li {
                                                icon("bullhorn".fas(), baseClass = "mr-sm".util())
                                                a {
                                                    +"Release notes"
                                                    href(releaseNotes)
                                                    target("release_notes")
                                                }
                                            }
                                        }
                                        if (documentation.isNotEmpty()) {
                                            li {
                                                icon("book".fas(), baseClass = "mr-sm".util())
                                                a {
                                                    +"Documentation"
                                                    href(documentation)
                                                    target("documentation")
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
