plugins {
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

dependencies {
    // Internal dependencies (proprietary)
    implementation(project(":migration-core"))
    implementation(project(":mcp-server"))
    implementation(project(":intellij-plugin"))

    // IntelliJ Platform
    compileOnly(intellijPlatform("com.jetbrains.kotlin.k2oo"))
}

// NOTE: This module is PROPRIETARY and not covered by Apache License 2.0
// Only included when building with -PpremiumEnabled=true

intellijPlatform {
    pluginVerification {
        idDirectory = file("src/main/java/adrianmikula/jakartamigration")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
