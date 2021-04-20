package org.wildfly.modelgraph.browser

external val process: Process

external interface Process {
    val env: dynamic
}

object Environment {

    fun <T> failSafe(value: dynamic, default: () -> T): T = if (value == undefined) {
        default()
    } else {
        value.unsafeCast<T>()
    }
}
