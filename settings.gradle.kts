plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "jakarta-migration-parent"

// Community modules (Apache 2.0)
include("migration-core")
include("mcp-server")
include("intellij-plugin")

// Premium modules (JetBrains Marketplace - proprietary)
// Included when premium module directories exist (loaded by Gradle automatically)
// No include() needed - Gradle discovers modules with build.gradle.kts
