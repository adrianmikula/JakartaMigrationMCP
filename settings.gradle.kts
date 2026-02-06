plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "jakarta-migration-parent"

// Free community modules (Apache 2.0)
include("free-core")

// Premium modules (Proprietary - loaded conditionally)
// These modules contain premium features that require a commercial license
// They are only included if:
// 1. The directory exists (for development)
// 2. -PpremiumEnabled=true is set

if (file("premium-core").exists() && providers.gradleProperty("premiumEnabled").getOrElse("false").toBoolean()) {
    include("premium-core")
    include("premium-mcp")
    include("premium-intellij-plugin")
}
