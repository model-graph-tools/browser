package org.wildfly.modelgraph.browser

import dev.fritz2.mvp.Presenter
import dev.fritz2.mvp.View
import dev.fritz2.mvp.ViewContent
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
            }
        }
    }
}