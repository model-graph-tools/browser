plugins {
    kotlin("js") version "1.5.31"
    kotlin("plugin.serialization") version "1.5.30"
}

group = "org.wildfly.modelgraph"
version = "0.0.1"

object Versions {
    // dependencies
    const val fritz2 = "0.12"
    const val mvp = "0.3.0"
    const val patternflyFritz2 = "0.3.0-SNAPSHOT"
    const val serialization = "1.3.0"

    // NPM (dev) dependencies
    const val fileLoader = "6.2.0"
    const val patternfly = "4"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.serialization}")
    implementation("dev.fritz2:core:${Versions.fritz2}")
    implementation("dev.fritz2:mvp:${Versions.mvp}")
    implementation("org.patternfly:patternfly-fritz2:${Versions.patternflyFritz2}")
    implementation(npm("@patternfly/patternfly", Versions.patternfly))
    implementation(devNpm("file-loader", Versions.fileLoader))
}

kotlin {
    js(IR) {
        sourceSets {
            named("main") {
                languageSettings.apply {
                    optIn("kotlin.ExperimentalStdlibApi")
                    optIn("kotlin.time.ExperimentalTime")
                    optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                    optIn("kotlinx.coroutines.FlowPreview")
                    optIn("kotlinx.serialization.ExperimentalSerializationApi")
                }
            }
        }
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
        }
        binaries.executable()
    }
}

// workaround for https://github.com/webpack/webpack-cli/issues/2990
rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().apply {
        resolution("@webpack-cli/serve", "1.5.2")
    }
}
