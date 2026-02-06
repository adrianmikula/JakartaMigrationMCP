plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "jakarta-migration-parent"

// Free modules (Apache 2.0)
include("free-core-engine")
include("free-mcp-server")
include("free-intellij-plugin")

// Premium modules (Proprietary - JetBrains Marketplace)
// Included when premium module directories exist (loaded by Gradle automatically)
// No include() needed - Gradle discovers modules with build.gradle.kts
