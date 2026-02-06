plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "jakarta-migration-parent"

// Community modules (Apache 2.0)
include("community-core-engine")
include("community-mcp-server")

// Premium modules (Proprietary)
include("premium-intellij-plugin")
