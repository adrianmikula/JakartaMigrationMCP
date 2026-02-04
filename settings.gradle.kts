plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "jakarta-migration-parent"

// Community modules (Apache 2.0)
include("migration-core")
include("mcp-server")
include("intellij-plugin")

// Premium modules (JetBrains Marketplace - proprietary)
// These will be included when premium engine is implemented
// include("premium-engine")
// include("premium-intellij")
