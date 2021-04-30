package org.wildfly.modelgraph.browser

interface BootstrapTask {
    val name: String
    suspend fun execute()
}

class ReadRegistry(private val dispatcher: Dispatcher, private val registry: Registry) : BootstrapTask {

    override val name: String = "read-registry"

    override suspend fun execute() {
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
        } else {
            registry.selectNone(Unit)
        }
    }
}
