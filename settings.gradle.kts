plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "jakarta-migration-parent"

// Community modules (Apache 2.0)
include("migration-core")
include("mcp-server")
include("intellij-plugin")

// Premium modules (Proprietary - loaded conditionally)
// These modules contain premium features that require a commercial license
// They are only included if the directory exists (for development)
// and when -PpremiumEnabled=true is set

if (file("premium-core").exists()) {
    include("premium-core")
    include("premium-mcp")
}
