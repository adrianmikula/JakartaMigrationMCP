plugins {
    java
    id("org.springframework.boot") version "3.2.0" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    id("org.jetbrains.intellij") version "1.17.2" apply false
}

allprojects {
    group = "adrianmikula"
    version = "1.0.0"

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
        maven { url = uri("https://repo.spring.io/snapshot") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
        maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
        maven { url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies") }
    }
}

// =============================================================================
// MODULE STRUCTURE
// =============================================================================
//
// Community Modules (Apache 2.0):
// - migration-core: Base analysis and scanning logic
// - mcp-server: MCP server with community tools (analyzeJakartaReadiness, detectBlockers, recommendVersions)
//
// Premium Module (Proprietary):
// - intellij-plugin: IntelliJ plugin with free trial ($49/mo or $399/yr after trial)
//
// The MCP server is fully open source (Apache 2.0).
// The IntelliJ plugin is proprietary and includes premium features.
//
