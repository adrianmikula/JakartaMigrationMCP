plugins {
    id("org.jetbrains.intellij")
    java
}

dependencies {
    implementation(project(":migration-core"))
    
    // Note: UI testing dependencies removed due to availability issues
    // Can be added later when needed for UI testing
}

intellij {
    version.set("2023.3.4")
    type.set("IC")
    plugins.set(listOf("com.intellij.java"))
}

tasks {
    patchPluginXml {
        sinceBuild.set("233")
        untilBuild.set("243.*")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
