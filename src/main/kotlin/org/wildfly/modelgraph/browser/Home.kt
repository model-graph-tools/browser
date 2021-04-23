package org.wildfly.modelgraph.browser

import dev.fritz2.mvp.Presenter
import dev.fritz2.mvp.View
import dev.fritz2.mvp.ViewContent
import org.patternfly.card
import org.patternfly.cardBody
import org.patternfly.cardHeader
import org.patternfly.cardTitle
import org.patternfly.cardView
import org.patternfly.classes
import org.patternfly.modifier
import org.patternfly.pageSection
import org.patternfly.textContent
import org.patternfly.title

class HomePresenter : Presenter<HomeView> {
    override val view: HomeView = HomeView()
}

class HomeView : View {
    override val content: ViewContent = {
        pageSection(baseClass = "light".modifier()) {
            textContent {
                title { +"WildFly Model Graph Browser" }
                p { +"Query and browse the WildFly management model." }
                p { +"The cards below show the available WildFly versions. Please select a version you want to work with. You can switch the active version anytime using the context selector in the header." }
            }
        }
        pageSection(baseClass = classes("fill".modifier())) {
            cardView(cdi().registry, singleSelection = true) {
                display { registration ->
                    card(registration, selectable = true, baseClass = "hoverable".modifier()) {
                        cardTitle { +"${registration.productName} ${registration.productVersion}" }
                        cardBody { +"Management model ${registration.managementVersion}" }
                    }
                }
            }
        }
    }
}
