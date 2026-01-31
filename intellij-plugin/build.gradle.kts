plugins {
    id("org.jetbrains.intellij")
    java
}

dependencies {
    implementation(project(":migration-core"))
    
    // UI Testing
    testImplementation("com.intellij.remoterobot:remote-robot-intellij:0.11.23")
    testImplementation("com.intellij.remoterobot:remote-fixtures:0.11.23")
}

intellij {
    version.set("2023.3.4")
    type.set("IC")
    plugins.set(listOf("com.intellij.java", "intellij.ml.llm"))
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
