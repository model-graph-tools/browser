plugins {
    kotlin("js") version "1.5.10"
    kotlin("plugin.serialization") version "1.5.10"
}

group = "org.wildfly.modelgraph"
version = "0.0.1"

object Versions {
    // dependencies
    const val fritz2 = "0.9.2"
    const val mvp = "0.3.0"
    const val patternflyFritz2 = "0.3.0-SNAPSHOT"
    const val serialization = "1.2.1"

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
                    useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
                    useExperimentalAnnotation("kotlin.time.ExperimentalTime")
                    useExperimentalAnnotation("kotlinx.coroutines.ExperimentalCoroutinesApi")
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
