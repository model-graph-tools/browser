package org.wildfly.modelgraph.browser

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val SINGLETON_PARENT_TYPE: String = "SingletonParentResource"

val json = Json { classDiscriminator = "modelType" }

interface Typed {
    val type: String
    val valueType: String?
}

interface WithDeprecated {
    val deprecation: Deprecation?

    val deprecated: Boolean
        get() = deprecation != null
}

@Serializable
sealed class Model {
    abstract val id: String
    abstract val name: String
    abstract val modelType: String
}

@Serializable
data class Models(
    val attributes: List<Attribute> = emptyList(),
    val capabilities: List<Capability> = emptyList(),
    val operations: List<Operation> = emptyList(),
    val resources: List<Resource> = emptyList()
) {

    val models: List<Model>
        get() = attributes + capabilities + operations + resources

    val size: Int
        get() = attributes.size + capabilities.size + operations.size + resources.size

    override fun toString(): String =
        "Models(a(${attributes.size}),c(${capabilities.size}),o(${operations.size}),r(${resources.size})"
}

@Serializable
@SerialName("Attribute")
data class Attribute(
    override val id: String,
    override val name: String,
    override val modelType: String,

    // required properties
    override val type: String,

    // optional properties
    val accessType: String? = null,
    val alias: String? = null,
    val attributeGroup: String? = null,
    val defaultValue: String? = null,
    val description: String? = null,
    val expressionAllowed: Boolean = false,
    val max: Long = 0,
    val maxLength: Long = 0,
    val min: Long = 0,
    val minLength: Long = 0,
    val nillable: Boolean = false,
    val required: Boolean = false,
    val restartRequired: String? = null,
    val since: String? = null,
    val storage: String? = null,
    val unit: String? = null,
    override val valueType: String? = null,
    override val deprecation: Deprecation? = null,

    // references
    val attributes: List<Attribute> = emptyList(),
    val alternatives: List<String> = emptyList(),
    val capability: String? = null,
    val definedIn: String? = null,
    val requires: List<String> = emptyList(),
) : Model(), Typed, WithDeprecated

@Serializable
@SerialName("Capability")
data class Capability(
    override val id: String,
    override val name: String,
    override val modelType: String,

    val declaredBy: List<String> = emptyList(),
) : Model()

@Serializable
@SerialName("Deprecation")
data class Deprecation(
    override val modelType: String,

    val reason: String,
    val since: Version
) : Model() {

    override val id: String = ""
    override val name: String = ""
}

@Serializable
@SerialName("Operation")
data class Operation(
    override val id: String,
    override val name: String,
    override val modelType: String,

    // required properties
    val global: Boolean,

    // optional properties
    val description: String? = null,
    val readOnly: Boolean = false,
    val returnValue: String? = null,
    val runtimeOnly: Boolean = false,
    override val deprecation: Deprecation? = null,

    // relations
    val parameters: List<Parameter> = emptyList(),
    val providedBy: String? = null,
) : Model(), WithDeprecated {

    companion object {
        var globalOperations: List<Operation> = emptyList()
    }
}

@Serializable
@SerialName("Parameter")
data class Parameter(
    override val id: String,
    override val name: String,
    override val modelType: String,

    // required properties
    override val type: String,

    // optional properties
    val description: String? = null,
    val expressionAllowed: Boolean = false,
    val max: Long = 0,
    val maxLength: Long = 0,
    val min: Long = 0,
    val minLength: Long = 0,
    val nillable: Boolean = false,
    val required: Boolean = false,
    val since: String? = null,
    val unit: String? = null,
    override val valueType: String? = null,
    override val deprecation: Deprecation? = null,

    // references
    val alternatives: List<String> = emptyList(),
    val capability: String? = null,
    val parameters: List<Parameter> = emptyList(),
    val requires: List<String> = emptyList(),
) : Model(), Typed, WithDeprecated

@Serializable
@SerialName("Resource")
data class Resource(
    override val id: String,
    override val name: String,
    override val modelType: String,

    // required properties
    val address: String,
    val singleton: Boolean,
    val childDescriptions: Map<String, String> = emptyMap(),

    // optional properties
    val description: String? = null,
    override val deprecation: Deprecation? = null,

    // references
    val parent: Resource? = null,
    val children: List<Resource> = emptyList(),
    val attributes: List<Attribute> = emptyList(),
    val capabilities: List<Capability> = emptyList(),
    val operations: List<Operation> = emptyList(),
) : Model(), WithDeprecated {

    val singletonParent: Boolean
        get() = modelType == SINGLETON_PARENT_TYPE

    val singletonParentName: String
        get() = if (singleton) name.substringBefore('=') else name

    val singletonChildName: String
        get() = if (singleton) name.substringAfter('=') else name

    override fun toString(): String = buildString {
        append("Resource(")
        append(id)
        append(", ")
        append(address)
        if (singletonParent) {
            append(", singleton-parent")
        } else if (singleton) {
            append(", singleton")
        }
        append(")")
    }
}

@Serializable
@SerialName("Version")
data class Version(
    override val id: String,
    override val modelType: String,

    val major: Int,
    val minor: Int,
    val patch: Int,
) : Model() {

    override val name: String = ""

    override fun toString(): String = "$major.$minor.$patch"
}
