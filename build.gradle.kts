plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = "com.consoleemulationtoggle"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // Build against the oldest supported PyCharm so the compile classpath
    // cannot accidentally pull in APIs that don't exist in 2024.3. Runtime
    // compatibility through newer builds is declared via untilBuild below.
    intellijPlatform {
        pycharmProfessional("2024.3")
        bundledPlugin("PythonCore")
        instrumentationTools()
    }
}

kotlin {
    jvmToolchain(21)
}

tasks {
    buildSearchableOptions {
        enabled = false
    }

    patchPluginXml {
        sinceBuild.set("243")
        untilBuild.set("262.*")
    }
}
